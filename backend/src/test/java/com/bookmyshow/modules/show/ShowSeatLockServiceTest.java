package com.bookmyshow.modules.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.show.SeatLockStore.HeldLock;
import com.bookmyshow.modules.show.SeatLockStore.LockResult;
import com.bookmyshow.modules.show.ShowTypes.ActiveLock;
import com.bookmyshow.modules.show.ShowTypes.SeatLockResponse;
import com.bookmyshow.modules.show.ShowTypes.SeatMapResponse;
import com.bookmyshow.modules.show.ShowTypes.SeatMapRow;
import com.bookmyshow.modules.show.ShowTypes.SeatMapSeat;
import com.bookmyshow.modules.show.ShowTypes.SeatStatus;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShowSeatLockServiceTest {

    // 2026-10-18 10:00 in India.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T04:30:00Z"), ZoneId.of("Asia/Kolkata"));
    private static final CurrentUser ASHA = new CurrentUser(7L, Roles.USER);

    private ShowSeatMapService seatMapService;
    private SeatLockStore seatLockStore;
    private ShowSeatLockService lockService;

    @BeforeEach
    void setUp() {
        seatMapService = mock(ShowSeatMapService.class);
        seatLockStore = mock(SeatLockStore.class);
        lockService = new ShowSeatLockService(seatMapService, seatLockStore, CLOCK);
        when(seatMapService.getSeatMap(50L)).thenReturn(map(LocalTime.of(18, 0)));
    }

    @Test
    void locksSeatsAndAddsUpPrice() {
        when(seatLockStore.lock(anyString(), eq(7L), eq(50L), eq(List.of(1L, 3L)), eq(ShowSeatLockService.LOCK_TTL)))
                .thenReturn(LockResult.LOCKED);

        SeatLockResponse response = lockService.lock(50L, List.of(1L, 3L), ASHA);

        assertThat(response.totalPrice()).isEqualByComparingTo("650.00");
        assertThat(response.seatIds()).containsExactly(1L, 3L);
        assertThat(response.expiresAt()).isEqualTo(Instant.parse("2026-10-18T04:40:00Z"));
        assertThat(response.lockId()).hasSize(36);
    }

    @Test
    void rejectsRepeatedSeat() {
        assertThatThrownBy(() -> lockService.lock(50L, List.of(1L, 1L), ASHA))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A seat is listed more than once");
        verifyNoInteractions(seatLockStore);
    }

    @Test
    void rejectsSeatFromAnotherScreen() {
        assertThatThrownBy(() -> lockService.lock(50L, List.of(99L), ASHA))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Seat 99 is not in show 50");
    }

    @Test
    void rejectsSeatWithoutPrice() {
        assertThatThrownBy(() -> lockService.lock(50L, List.of(4L), ASHA))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Seat 4 is not for sale");
    }

    @Test
    void rejectsSeatAlreadyLocked() {
        assertThatThrownBy(() -> lockService.lock(50L, List.of(2L), ASHA))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Seat 2 is already taken");
        verify(seatLockStore, never()).lock(anyString(), anyLong(), anyLong(), anyList(), any());
    }

    @Test
    void reportsLostRace() {
        when(seatLockStore.lock(anyString(), anyLong(), anyLong(), anyList(), any()))
                .thenReturn(LockResult.SEAT_TAKEN);

        assertThatThrownBy(() -> lockService.lock(50L, List.of(1L), ASHA))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A seat was just taken by someone else. Pick again.");
    }

    @Test
    void allowsOneLockPerUserPerShow() {
        when(seatLockStore.lock(anyString(), anyLong(), anyLong(), anyList(), any()))
                .thenReturn(LockResult.USER_HAS_LOCK);

        assertThatThrownBy(() -> lockService.lock(50L, List.of(1L), ASHA))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Release them first");
    }

    @Test
    void rejectsStartedShow() {
        when(seatMapService.getSeatMap(50L)).thenReturn(map(LocalTime.of(9, 0)));

        assertThatThrownBy(() -> lockService.lock(50L, List.of(1L), ASHA))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Show 50 has already started");
    }

    @Test
    void ownerReleasesLock() {
        HeldLock held = new HeldLock("lock-1", 7L, 50L, List.of(1L));
        when(seatLockStore.find("lock-1")).thenReturn(Optional.of(held));

        lockService.release("lock-1", ASHA);

        verify(seatLockStore).release(held);
    }

    @Test
    void otherUserCannotRelease() {
        when(seatLockStore.find("lock-1")).thenReturn(Optional.of(new HeldLock("lock-1", 8L, 50L, List.of(1L))));

        assertThatThrownBy(() -> lockService.release("lock-1", ASHA)).isInstanceOf(ForbiddenException.class);
        verify(seatLockStore, never()).release(any());
    }

    @Test
    void releaseOfExpiredLockIs404() {
        when(seatLockStore.find("lock-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lockService.release("lock-1", ASHA)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsBookedSeat() {
        assertThatThrownBy(() -> lockService.lock(50L, List.of(5L), ASHA))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Seat 5 is already booked");
    }

    @Test
    void activeLockCarriesCurrentPrice() {
        when(seatLockStore.find("lock-1")).thenReturn(Optional.of(new HeldLock("lock-1", 7L, 50L, List.of(1L, 3L))));

        ActiveLock lock = lockService.getActiveLock("lock-1", ASHA);

        assertThat(lock.showId()).isEqualTo(50L);
        assertThat(lock.totalPrice()).isEqualByComparingTo("650.00");
    }

    @Test
    void activeLockFailsIfSeatWasSold() {
        when(seatLockStore.find("lock-1")).thenReturn(Optional.of(new HeldLock("lock-1", 7L, 50L, List.of(5L))));

        assertThatThrownBy(() -> lockService.getActiveLock("lock-1", ASHA)).isInstanceOf(ConflictException.class);
    }

    @Test
    void activeLockOfOtherUserIsForbidden() {
        when(seatLockStore.find("lock-1")).thenReturn(Optional.of(new HeldLock("lock-1", 8L, 50L, List.of(1L))));

        assertThatThrownBy(() -> lockService.getActiveLock("lock-1", ASHA)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void releaseAfterSaleIgnoresExpiredLock() {
        when(seatLockStore.find("lock-1")).thenReturn(Optional.empty());

        lockService.releaseAfterSale("lock-1");

        verify(seatLockStore, never()).release(any());
    }

    /** Seats: 1 regular 200, 2 regular locked, 3 recliner 450, 4 premium without price, 5 sold. Show is today. */
    private static SeatMapResponse map(LocalTime startTime) {
        return new SeatMapResponse(
                50L,
                2L,
                "Sky Riders",
                LocalDate.of(2026, 10, 18),
                startTime,
                3L,
                "Screen 1",
                List.of(),
                List.of(
                        new SeatMapRow(
                                "A",
                                List.of(
                                        new SeatMapSeat(
                                                1L, 1, "REGULAR", new BigDecimal("200.00"), SeatStatus.AVAILABLE),
                                        new SeatMapSeat(
                                                2L, 2, "REGULAR", new BigDecimal("200.00"), SeatStatus.LOCKED))),
                        new SeatMapRow(
                                "J",
                                List.of(
                                        new SeatMapSeat(
                                                3L, 1, "RECLINER", new BigDecimal("450.00"), SeatStatus.AVAILABLE),
                                        new SeatMapSeat(4L, 2, "PREMIUM", null, SeatStatus.UNAVAILABLE),
                                        new SeatMapSeat(
                                                5L, 3, "RECLINER", new BigDecimal("450.00"), SeatStatus.BOOKED)))));
    }
}
