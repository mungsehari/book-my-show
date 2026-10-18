package com.bookmyshow.modules.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.screen.ScreenService;
import com.bookmyshow.modules.screen.ScreenTypes.LayoutResponse;
import com.bookmyshow.modules.screen.ScreenTypes.RowResponse;
import com.bookmyshow.modules.screen.ScreenTypes.SeatResponse;
import com.bookmyshow.modules.show.ShowTypes.SeatMapResponse;
import com.bookmyshow.modules.show.ShowTypes.SeatMapSeat;
import com.bookmyshow.modules.show.ShowTypes.SeatStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ShowSeatMapServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 20);

    /** Two days before the test show at 2026-10-20 14:00, so no last-minute deal runs. */
    private static final Clock FAR_AWAY = Clock.fixed(Instant.parse("2026-10-18T05:00:00Z"), ZoneOffset.UTC);

    private ShowRepository showRepository;
    private ShowPriceRepository showPriceRepository;
    private ScreenService screenService;
    private SeatLockStore seatLockStore;
    private ShowSeatBookingService seatBookingService;
    private ShowSeatMapService seatMapService;

    @BeforeEach
    void setUp() {
        showRepository = mock(ShowRepository.class);
        showPriceRepository = mock(ShowPriceRepository.class);
        screenService = mock(ScreenService.class);
        seatLockStore = mock(SeatLockStore.class);
        seatBookingService = mock(ShowSeatBookingService.class);
        seatMapService = new ShowSeatMapService(
                showRepository, showPriceRepository, screenService, seatLockStore, seatBookingService, FAR_AWAY);
    }

    @Test
    void buildsSeatMapWithPrices() {
        when(showRepository.findById(50L)).thenReturn(Optional.of(show(3L)));
        when(screenService.getLayout(3L))
                .thenReturn(new LayoutResponse(
                        3L,
                        10L,
                        "Screen 1",
                        3,
                        List.of(
                                new RowResponse(
                                        "A",
                                        List.of(
                                                new SeatResponse(1L, 1, "REGULAR", false),
                                                new SeatResponse(2L, 2, "REGULAR", false))),
                                new RowResponse("J", List.of(new SeatResponse(3L, 1, "RECLINER", false))))));
        when(showPriceRepository.findAllByShowId(50L))
                .thenReturn(List.of(
                        new ShowPrice(50L, "RECLINER", new BigDecimal("450.00")),
                        new ShowPrice(50L, "REGULAR", new BigDecimal("200.00"))));

        SeatMapResponse map = seatMapService.getSeatMap(50L);

        assertThat(map.screenName()).isEqualTo("Screen 1");
        assertThat(map.prices()).extracting("category").containsExactly("REGULAR", "RECLINER");
        SeatMapSeat recliner = map.rows().get(1).seats().get(0);
        assertThat(recliner.price()).isEqualByComparingTo("450.00");
        assertThat(recliner.status()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(map.rows().get(0).seats()).hasSize(2);
    }

    @Test
    void soldSeatIsBookedEvenIfAlsoLocked() {
        when(showRepository.findById(50L)).thenReturn(Optional.of(show(3L)));
        when(screenService.getLayout(3L))
                .thenReturn(new LayoutResponse(
                        3L,
                        10L,
                        "Screen 1",
                        2,
                        List.of(new RowResponse(
                                "A",
                                List.of(
                                        new SeatResponse(1L, 1, "REGULAR", false),
                                        new SeatResponse(2L, 2, "REGULAR", false))))));
        when(showPriceRepository.findAllByShowId(50L))
                .thenReturn(List.of(new ShowPrice(50L, "REGULAR", new BigDecimal("200.00"))));
        when(seatBookingService.soldSeats(50L)).thenReturn(Set.of(1L));
        when(seatLockStore.lockedSeats(50L, List.of(1L, 2L))).thenReturn(Set.of(1L));

        List<SeatMapSeat> seats = seatMapService.getSeatMap(50L).rows().get(0).seats();

        assertThat(seats).extracting(SeatMapSeat::status).containsExactly(SeatStatus.BOOKED, SeatStatus.AVAILABLE);
    }

    @Test
    void heldSeatIsLocked() {
        when(showRepository.findById(50L)).thenReturn(Optional.of(show(3L)));
        when(screenService.getLayout(3L))
                .thenReturn(new LayoutResponse(
                        3L,
                        10L,
                        "Screen 1",
                        2,
                        List.of(new RowResponse(
                                "A",
                                List.of(
                                        new SeatResponse(1L, 1, "REGULAR", false),
                                        new SeatResponse(2L, 2, "REGULAR", false))))));
        when(showPriceRepository.findAllByShowId(50L))
                .thenReturn(List.of(new ShowPrice(50L, "REGULAR", new BigDecimal("200.00"))));
        when(seatLockStore.lockedSeats(50L, List.of(1L, 2L))).thenReturn(Set.of(2L));

        List<SeatMapSeat> seats = seatMapService.getSeatMap(50L).rows().get(0).seats();

        assertThat(seats).extracting(SeatMapSeat::status).containsExactly(SeatStatus.AVAILABLE, SeatStatus.LOCKED);
    }

    @Test
    void seatWithoutPriceIsUnavailable() {
        when(showRepository.findById(50L)).thenReturn(Optional.of(show(3L)));
        when(screenService.getLayout(3L))
                .thenReturn(new LayoutResponse(
                        3L,
                        10L,
                        "Screen 1",
                        1,
                        List.of(new RowResponse("G", List.of(new SeatResponse(7L, 1, "PREMIUM", false))))));
        when(showPriceRepository.findAllByShowId(50L)).thenReturn(List.of());

        SeatMapSeat seat = seatMapService.getSeatMap(50L).rows().get(0).seats().get(0);

        assertThat(seat.price()).isNull();
        assertThat(seat.status()).isEqualTo(SeatStatus.UNAVAILABLE);
    }

    @Test
    void failsForShowWithoutScreen() {
        when(showRepository.findById(50L)).thenReturn(Optional.of(show(null)));

        assertThatThrownBy(() -> seatMapService.getSeatMap(50L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Show 50 has no seat map, because it is not on a screen");
        verifyNoInteractions(screenService);
    }

    @Test
    void failsForMissingShow() {
        when(showRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seatMapService.getSeatMap(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Show 99 not found");
    }

    @Test
    void lastHourDealLowersPricesAndShowsOriginal() {
        // The show is 2026-10-20 14:00. The clock is 13:30, and nothing is sold: the deal runs.
        ShowSeatMapService lastHour = new ShowSeatMapService(
                showRepository,
                showPriceRepository,
                screenService,
                seatLockStore,
                seatBookingService,
                Clock.fixed(Instant.parse("2026-10-20T13:30:00Z"), ZoneOffset.UTC));
        stubOneRegularSeat();

        SeatMapResponse map = lastHour.getSeatMap(50L);

        assertThat(map.deal()).isNotNull();
        assertThat(map.deal().percentOff()).isEqualTo(20);
        assertThat(map.rows().get(0).seats().get(0).price()).isEqualByComparingTo("160.00");
        assertThat(map.prices().get(0).price()).isEqualByComparingTo("160.00");
        assertThat(map.prices().get(0).originalPrice()).isEqualByComparingTo("200.00");
        assertThat(lastHour.lowestAvailablePrice(50L))
                .hasValueSatisfying(price -> assertThat(price).isEqualByComparingTo("160.00"));
    }

    @Test
    void noFreeSeatMeansNoLowestPrice() {
        stubOneRegularSeat();
        when(seatBookingService.soldSeats(50L)).thenReturn(Set.of(1L));

        assertThat(seatMapService.lowestAvailablePrice(50L)).isEmpty();
        assertThat(seatMapService.getSeatMap(50L).deal()).isNull();
    }

    private void stubOneRegularSeat() {
        when(showRepository.findById(50L)).thenReturn(Optional.of(show(3L)));
        when(screenService.getLayout(3L))
                .thenReturn(new LayoutResponse(
                        3L,
                        10L,
                        "Screen 1",
                        1,
                        List.of(new RowResponse("A", List.of(new SeatResponse(1L, 1, "REGULAR", false))))));
        when(showPriceRepository.findAllByShowId(50L))
                .thenReturn(List.of(new ShowPrice(50L, "REGULAR", new BigDecimal("200.00"))));
    }

    private static Show show(Long screenId) {
        Show show = new Show(10L, screenId, 2L, "Sky Riders", DATE, LocalTime.of(14, 0), new BigDecimal("200.00"));
        ReflectionTestUtils.setField(show, "id", 50L);
        return show;
    }
}
