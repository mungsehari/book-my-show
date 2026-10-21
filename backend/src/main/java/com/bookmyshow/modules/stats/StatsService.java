package com.bookmyshow.modules.stats;

import com.bookmyshow.common.response.PageResponse;
import com.bookmyshow.modules.booking.BookingStatsService;
import com.bookmyshow.modules.booking.BookingTypes.BookingResponse;
import com.bookmyshow.modules.booking.BookingTypes.SaleFact;
import com.bookmyshow.modules.cinema.CinemaAccessService;
import com.bookmyshow.modules.show.ShowCrowdService;
import com.bookmyshow.modules.show.ShowService;
import com.bookmyshow.modules.show.ShowTypes.CrowdResponse;
import com.bookmyshow.modules.stats.StatsTypes.DailyPoint;
import com.bookmyshow.modules.stats.StatsTypes.DailyResponse;
import com.bookmyshow.modules.stats.StatsTypes.OccupancyResponse;
import com.bookmyshow.modules.stats.StatsTypes.ShowOccupancy;
import com.bookmyshow.modules.stats.StatsTypes.SummaryResponse;
import com.bookmyshow.modules.stats.StatsTypes.TopMovie;
import com.bookmyshow.modules.stats.StatsTypes.TopMoviesResponse;
import com.bookmyshow.security.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Numbers for the admin dashboard. Admins see every cinema. Theatre admins see only the cinemas assigned to them. Days
 * are counted in the server time zone.
 */
@Service
public class StatsService {

    private final CinemaAccessService cinemaAccessService;
    private final BookingStatsService bookingStatsService;
    private final ShowService showService;
    private final ShowCrowdService crowdService;
    private final Clock clock;

    public StatsService(
            CinemaAccessService cinemaAccessService,
            BookingStatsService bookingStatsService,
            ShowService showService,
            ShowCrowdService crowdService,
            Clock clock) {
        this.cinemaAccessService = cinemaAccessService;
        this.bookingStatsService = bookingStatsService;
        this.showService = showService;
        this.crowdService = crowdService;
        this.clock = clock;
    }

    public SummaryResponse summary(CurrentUser user, int days) {
        List<Long> cinemaIds = cinemaAccessService.managedCinemaIds(user);
        Instant from = windowStart(days);
        List<SaleFact> sales = bookingStatsService.sales(cinemaIds, from);
        BigDecimal revenue = revenue(sales);
        long tickets = tickets(sales);
        BigDecimal average = tickets == 0
                ? BigDecimal.ZERO.setScale(2)
                : revenue.divide(BigDecimal.valueOf(tickets), 2, RoundingMode.HALF_UP);
        return new SummaryResponse(
                days,
                sales.size(),
                tickets,
                revenue,
                average,
                bookingStatsService.cancelledSince(cinemaIds, from),
                bookingStatsService.refundsPending(cinemaIds));
    }

    public DailyResponse daily(CurrentUser user, int days) {
        List<SaleFact> sales = bookingStatsService.sales(cinemaAccessService.managedCinemaIds(user), windowStart(days));
        Map<LocalDate, List<SaleFact>> byDay = sales.stream()
                .collect(Collectors.groupingBy(
                        sale -> sale.confirmedAt().atZone(clock.getZone()).toLocalDate()));
        LocalDate today = LocalDate.now(clock);
        Map<LocalDate, DailyPoint> points = new TreeMap<>();
        for (int back = days - 1; back >= 0; back--) {
            LocalDate date = today.minusDays(back);
            List<SaleFact> day = byDay.getOrDefault(date, List.of());
            points.put(date, new DailyPoint(date, day.size(), tickets(day), revenue(day)));
        }
        return new DailyResponse(days, List.copyOf(points.values()));
    }

    public TopMoviesResponse topMovies(CurrentUser user, int days, int limit) {
        List<SaleFact> sales = bookingStatsService.sales(cinemaAccessService.managedCinemaIds(user), windowStart(days));
        List<TopMovie> top = sales.stream().collect(Collectors.groupingBy(SaleFact::movieName)).entrySet().stream()
                .map(entry -> new TopMovie(entry.getKey(), tickets(entry.getValue()), revenue(entry.getValue())))
                .sorted(Comparator.comparing(TopMovie::revenue).reversed().thenComparing(TopMovie::movieName))
                .limit(limit)
                .toList();
        return new TopMoviesResponse(days, top);
    }

    /** How full each show is on a date, soonest first. */
    public OccupancyResponse occupancy(CurrentUser user, LocalDate date) {
        List<Long> cinemaIds = cinemaAccessService.managedCinemaIds(user);
        List<ShowOccupancy> items = showService.listInCinemasOnDate(cinemaIds, date).stream()
                .map(show -> {
                    CrowdResponse crowd = crowdService.getCrowd(show.id());
                    return new ShowOccupancy(
                            show.id(),
                            show.cinemaId(),
                            show.movieName(),
                            show.showDate(),
                            show.startTime(),
                            crowd.totalSeats(),
                            crowd.sold(),
                            crowd.held(),
                            crowd.percentFull(),
                            crowd.level());
                })
                .toList();
        return new OccupancyResponse(date, items);
    }

    /** All bookings in the admin's cinemas, newest first. status is optional. */
    public PageResponse<BookingResponse> bookings(CurrentUser user, String status, int page, int size) {
        return bookingStatsService.list(cinemaAccessService.managedCinemaIds(user), status, page, size);
    }

    /** The start of the first day in the window. A 7 day window is today and the 6 days before it. */
    private Instant windowStart(int days) {
        return LocalDate.now(clock)
                .minusDays(days - 1L)
                .atStartOfDay(clock.getZone())
                .toInstant();
    }

    private static BigDecimal revenue(List<SaleFact> sales) {
        return sales.stream()
                .map(SaleFact::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static long tickets(List<SaleFact> sales) {
        return sales.stream().mapToLong(SaleFact::tickets).sum();
    }
}
