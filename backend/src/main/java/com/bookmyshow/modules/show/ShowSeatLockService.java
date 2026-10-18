package com.bookmyshow.modules.show;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.show.SeatLockStore.HeldLock;
import com.bookmyshow.modules.show.SeatLockStore.LockResult;
import com.bookmyshow.modules.show.ShowTypes.SeatLockResponse;
import com.bookmyshow.modules.show.ShowTypes.SeatMapResponse;
import com.bookmyshow.modules.show.ShowTypes.SeatMapSeat;
import com.bookmyshow.modules.show.ShowTypes.SeatStatus;
import com.bookmyshow.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Holds seats for a user during checkout. The checks here give clear errors. The Redis script is the real guard, so
 * two users racing for the same seat can never both win.
 */
@Service
public class ShowSeatLockService {

    /** How long seats stay held. */
    static final Duration LOCK_TTL = Duration.ofMinutes(10);

    private final ShowSeatMapService seatMapService;
    private final SeatLockStore seatLockStore;
    private final Clock clock;

    public ShowSeatLockService(ShowSeatMapService seatMapService, SeatLockStore seatLockStore, Clock clock) {
        this.seatMapService = seatMapService;
        this.seatLockStore = seatLockStore;
        this.clock = clock;
    }

    public SeatLockResponse lock(long showId, List<Long> seatIds, CurrentUser user) {
        if (new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new BadRequestException("A seat is listed more than once");
        }
        SeatMapResponse map = seatMapService.getSeatMap(showId);
        if (!LocalDateTime.of(map.showDate(), map.startTime()).isAfter(LocalDateTime.now(clock))) {
            throw new BadRequestException("Show " + showId + " has already started");
        }

        Map<Long, SeatMapSeat> seats = map.rows().stream()
                .flatMap(row -> row.seats().stream())
                .collect(Collectors.toMap(SeatMapSeat::id, Function.identity()));
        BigDecimal total = BigDecimal.ZERO;
        for (Long seatId : seatIds) {
            SeatMapSeat seat = seats.get(seatId);
            if (seat == null) {
                throw new BadRequestException("Seat " + seatId + " is not in show " + showId);
            }
            if (SeatStatus.UNAVAILABLE.equals(seat.status())) {
                throw new BadRequestException("Seat " + seatId + " is not for sale");
            }
            if (SeatStatus.LOCKED.equals(seat.status())) {
                throw new ConflictException("Seat " + seatId + " is already taken");
            }
            total = total.add(seat.price());
        }

        String lockId = UUID.randomUUID().toString();
        LockResult result = seatLockStore.lock(lockId, user.id(), showId, seatIds, LOCK_TTL);
        if (result == LockResult.USER_HAS_LOCK) {
            throw new ConflictException("You already hold seats for show " + showId + ". Release them first.");
        }
        if (result == LockResult.SEAT_TAKEN) {
            throw new ConflictException("A seat was just taken by someone else. Pick again.");
        }
        Instant expiresAt = clock.instant().plus(LOCK_TTL).truncatedTo(ChronoUnit.SECONDS);
        return new SeatLockResponse(lockId, showId, List.copyOf(seatIds), total, expiresAt);
    }

    /** Frees the seats early. Only the user who holds the lock can release it. */
    public void release(String lockId, CurrentUser user) {
        HeldLock lock = seatLockStore
                .find(lockId)
                .orElseThrow(() -> new NotFoundException("Lock " + lockId + " not found or already expired"));
        if (lock.userId() != user.id()) {
            throw new ForbiddenException("You cannot release another user's seats");
        }
        seatLockStore.release(lock);
    }
}
