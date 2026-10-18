package com.bookmyshow.modules.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.modules.alert.PriceAlertTypes.CreatePriceAlertRequest;
import com.bookmyshow.modules.city.CityService;
import com.bookmyshow.modules.movie.MovieService;
import com.bookmyshow.modules.show.ShowSeatMapService;
import com.bookmyshow.modules.show.ShowService;
import com.bookmyshow.modules.show.ShowTypes.ShowResponse;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class PriceAlertServiceTest {

    private static final CurrentUser ASHA = new CurrentUser(7L, Roles.USER);

    private PriceAlertRepository alertRepository;
    private ShowService showService;
    private ShowSeatMapService seatMapService;
    private Notifier notifier;
    private PriceAlertService alertService;

    @BeforeEach
    void setUp() {
        alertRepository = mock(PriceAlertRepository.class);
        showService = mock(ShowService.class);
        seatMapService = mock(ShowSeatMapService.class);
        notifier = mock(Notifier.class);
        alertService = new PriceAlertService(
                alertRepository,
                mock(MovieService.class),
                mock(CityService.class),
                showService,
                seatMapService,
                notifier,
                Clock.fixed(Instant.parse("2026-10-18T05:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createsAlert() {
        when(alertRepository.save(any(PriceAlert.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(alertService.create(request("150.00"), ASHA).status()).isEqualTo(PriceAlertStatus.ACTIVE);
    }

    @Test
    void oneActiveAlertPerMovieAndCity() {
        when(alertRepository.existsByUserIdAndMovieIdAndCityIdAndStatus(7L, 2L, 1L, PriceAlertStatus.ACTIVE))
                .thenReturn(true);

        assertThatThrownBy(() -> alertService.create(request("150.00"), ASHA)).isInstanceOf(ConflictException.class);
    }

    @Test
    void atMostTenActiveAlerts() {
        when(alertRepository.countByUserIdAndStatus(7L, PriceAlertStatus.ACTIVE))
                .thenReturn(10L);

        assertThatThrownBy(() -> alertService.create(request("150.00"), ASHA)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void firesOnceForSoonestCheapShow() {
        PriceAlert cheap = alert(1L, 7L, "170.00");
        PriceAlert tooLow = alert(2L, 8L, "100.00");
        when(alertRepository.findByStatusAndIdGreaterThanOrderByIdAsc(
                        eq(PriceAlertStatus.ACTIVE), eq(0L), any(Pageable.class)))
                .thenReturn(List.of(cheap, tooLow));
        when(showService.listUpcomingForMovieInCity(2L, 1L)).thenReturn(List.of(show(50L, 18), show(51L, 21)));
        when(seatMapService.lowestAvailablePrice(50L)).thenReturn(Optional.of(new BigDecimal("200.00")));
        when(seatMapService.lowestAvailablePrice(51L)).thenReturn(Optional.of(new BigDecimal("160.00")));

        assertThat(alertService.runChecks()).isEqualTo(1);

        assertThat(cheap.getStatus()).isEqualTo(PriceAlertStatus.TRIGGERED);
        assertThat(cheap.getTriggeredShowId()).isEqualTo(51L);
        assertThat(cheap.getTriggeredPrice()).isEqualByComparingTo("160.00");
        assertThat(tooLow.getStatus()).isEqualTo(PriceAlertStatus.ACTIVE);
        verify(notifier).notify(eq(7L), anyString(), anyString());
        verify(notifier, never()).notify(eq(8L), anyString(), anyString());
        // Shows and prices are looked up once per run, not once per alert.
        verify(showService, times(1)).listUpcomingForMovieInCity(2L, 1L);
        verify(seatMapService, times(1)).lowestAvailablePrice(51L);
    }

    private static CreatePriceAlertRequest request(String maxPrice) {
        return new CreatePriceAlertRequest(2L, 1L, new BigDecimal(maxPrice));
    }

    private static PriceAlert alert(long id, long userId, String maxPrice) {
        PriceAlert alert = new PriceAlert(userId, 2L, 1L, new BigDecimal(maxPrice), Instant.now());
        ReflectionTestUtils.setField(alert, "id", id);
        return alert;
    }

    private static ShowResponse show(long id, int hour) {
        return new ShowResponse(
                id,
                10L,
                3L,
                2L,
                "Sky Riders",
                LocalDate.of(2026, 10, 18),
                LocalTime.of(hour, 0),
                new BigDecimal("200.00"),
                false,
                false,
                false);
    }
}
