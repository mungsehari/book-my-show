package com.bookmyshow.modules.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.modules.booking.BookingStatsService;
import com.bookmyshow.modules.booking.BookingTypes.SaleFact;
import com.bookmyshow.modules.cinema.CinemaAccessService;
import com.bookmyshow.modules.show.ShowCrowdService;
import com.bookmyshow.modules.show.ShowService;
import com.bookmyshow.modules.show.ShowTypes.CrowdResponse;
import com.bookmyshow.modules.show.ShowTypes.ShowResponse;
import com.bookmyshow.modules.stats.StatsTypes.DailyResponse;
import com.bookmyshow.modules.stats.StatsTypes.OccupancyResponse;
import com.bookmyshow.modules.stats.StatsTypes.SummaryResponse;
import com.bookmyshow.modules.stats.StatsTypes.TopMoviesResponse;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatsServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");
    private static final Instant WEEK_START = Instant.parse("2026-10-12T00:00:00Z");
    private static final CurrentUser OWNER = new CurrentUser(3L, Roles.THEATRE_ADMIN);
    private static final List<Long> MY_CINEMAS = List.of(10L, 11L);

    private CinemaAccessService cinemaAccessService;
    private BookingStatsService bookingStatsService;
    private ShowService showService;
    private ShowCrowdService crowdService;
    private StatsService service;

    @BeforeEach
    void setUp() {
        cinemaAccessService = mock(CinemaAccessService.class);
        bookingStatsService = mock(BookingStatsService.class);
        showService = mock(ShowService.class);
        crowdService = mock(ShowCrowdService.class);
        service = new StatsService(
                cinemaAccessService, bookingStatsService, showService, crowdService, Clock.fixed(NOW, ZoneOffset.UTC));
        when(cinemaAccessService.managedCinemaIds(OWNER)).thenReturn(MY_CINEMAS);
    }

    @Test
    void summaryAddsUpSalesAndAveragesPerTicket() {
        when(bookingStatsService.sales(MY_CINEMAS, WEEK_START))
                .thenReturn(List.of(
                        sale("2026-10-17T05:00:00Z", "300.00", "Sky Riders", 2),
                        sale("2026-10-18T08:00:00Z", "100.00", "Code Red", 2)));
        when(bookingStatsService.cancelledSince(MY_CINEMAS, WEEK_START)).thenReturn(1L);
        when(bookingStatsService.refundsPending(MY_CINEMAS)).thenReturn(2L);

        SummaryResponse summary = service.summary(OWNER, 7);

        assertThat(summary.bookings()).isEqualTo(2);
        assertThat(summary.ticketsSold()).isEqualTo(4);
        assertThat(summary.revenue()).isEqualByComparingTo("400.00");
        assertThat(summary.averageTicketPrice()).isEqualByComparingTo("100.00");
        assertThat(summary.cancelled()).isEqualTo(1);
        assertThat(summary.refundsPending()).isEqualTo(2);
    }

    @Test
    void summaryWithNoSalesIsAllZeros() {
        when(bookingStatsService.sales(MY_CINEMAS, WEEK_START)).thenReturn(List.of());

        SummaryResponse summary = service.summary(OWNER, 7);

        assertThat(summary.bookings()).isZero();
        assertThat(summary.revenue()).isEqualByComparingTo("0");
        assertThat(summary.averageTicketPrice()).isEqualByComparingTo("0");
    }

    @Test
    void dailyListsEveryDayWithZerosForQuietOnes() {
        when(bookingStatsService.sales(MY_CINEMAS, Instant.parse("2026-10-16T00:00:00Z")))
                .thenReturn(List.of(
                        sale("2026-10-16T23:30:00Z", "100.00", "Sky Riders", 1),
                        sale("2026-10-18T08:00:00Z", "250.00", "Sky Riders", 2),
                        sale("2026-10-18T09:00:00Z", "50.00", "Code Red", 1)));

        DailyResponse daily = service.daily(OWNER, 3);

        assertThat(daily.points())
                .extracting(point -> point.date())
                .containsExactly(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 18));
        assertThat(daily.points().get(0).revenue()).isEqualByComparingTo("100.00");
        assertThat(daily.points().get(1).bookings()).isZero();
        assertThat(daily.points().get(2).bookings()).isEqualTo(2);
        assertThat(daily.points().get(2).tickets()).isEqualTo(3);
        assertThat(daily.points().get(2).revenue()).isEqualByComparingTo("300.00");
    }

    @Test
    void topMoviesAreSortedByRevenueAndLimited() {
        when(bookingStatsService.sales(MY_CINEMAS, WEEK_START))
                .thenReturn(List.of(
                        sale("2026-10-17T05:00:00Z", "100.00", "Code Red", 1),
                        sale("2026-10-17T06:00:00Z", "300.00", "Sky Riders", 2),
                        sale("2026-10-17T07:00:00Z", "200.00", "Sky Riders", 1),
                        sale("2026-10-17T08:00:00Z", "50.00", "Monsoon", 1)));

        TopMoviesResponse top = service.topMovies(OWNER, 7, 2);

        assertThat(top.items()).extracting(item -> item.movieName()).containsExactly("Sky Riders", "Code Red");
        assertThat(top.items().get(0).revenue()).isEqualByComparingTo("500.00");
        assertThat(top.items().get(0).tickets()).isEqualTo(3);
    }

    @Test
    void occupancyUsesTheCrowdMeterForEachShow() {
        LocalDate date = LocalDate.of(2026, 10, 18);
        when(showService.listInCinemasOnDate(MY_CINEMAS, date))
                .thenReturn(List.of(show(50L, 10L, "Sky Riders", LocalTime.of(18, 0))));
        when(crowdService.getCrowd(50L)).thenReturn(new CrowdResponse(50L, 100, 60, 15, 25, 75, "HIGH"));

        OccupancyResponse occupancy = service.occupancy(OWNER, date);

        assertThat(occupancy.items()).hasSize(1);
        assertThat(occupancy.items().get(0).percentFull()).isEqualTo(75);
        assertThat(occupancy.items().get(0).level()).isEqualTo("HIGH");
        assertThat(occupancy.items().get(0).sold()).isEqualTo(60);
    }

    @Test
    void usersWhoAreNotAdminsAreRefused() {
        CurrentUser plain = new CurrentUser(9L, Roles.USER);
        when(cinemaAccessService.managedCinemaIds(plain)).thenThrow(new ForbiddenException("Admins only"));

        assertThatThrownBy(() -> service.summary(plain, 7)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void bookingsListUsesTheAdminsCinemas() {
        service.bookings(OWNER, "NEEDS_REFUND", 0, 20);

        verify(bookingStatsService).list(MY_CINEMAS, "NEEDS_REFUND", 0, 20);
    }

    private static SaleFact sale(String confirmedAt, String amount, String movie, int tickets) {
        return new SaleFact(Instant.parse(confirmedAt), new BigDecimal(amount), 1L, movie, tickets);
    }

    private static ShowResponse show(long id, long cinemaId, String movie, LocalTime time) {
        return new ShowResponse(
                id,
                cinemaId,
                5L,
                2L,
                movie,
                LocalDate.of(2026, 10, 18),
                time,
                new BigDecimal("200.00"),
                false,
                false,
                false);
    }
}
