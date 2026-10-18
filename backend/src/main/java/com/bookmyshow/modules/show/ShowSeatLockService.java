package com.bookmyshow.modules.show;

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
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final ShowRepository showRepository;
    private final Clock clock;

    public ShowSeatLockService(
            ShowSeatMapService seatMapService,
            SeatLockStore seatLockStore,
            ShowRepository showRepository,
            Clock clock) {
        this.seatMapService = seatMapService;
        this.seatLockStore = seatLockStore;
        this.showRepository = showRepository;
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
            if (SeatStatus.BOOKED.equals(seat.status())) {
                throw new ConflictException("Seat " + seatId + " is already booked");
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
        seatLockStore.release(ownedLock(lockId, user));
    }

    /**
     * Used by the booking module at checkout. Returns the user's lock with the price of its seats now. 404 if the lock
     * expired, 403 if it is someone else's, 409 if a seat was sold in the meantime.
     */
    public ActiveLock getActiveLock(String lockId, CurrentUser user) {
        HeldLock lock = ownedLock(lockId, user);
        SeatMapResponse map = seatMapService.getSeatMap(lock.showId());
        Map<Long, SeatMapSeat> seats = new HashMap<>();
        Map<Long, String> labels = new HashMap<>();
        for (SeatMapRow row : map.rows()) {
            for (SeatMapSeat seat : row.seats()) {
                seats.put(seat.id(), seat);
                labels.put(seat.id(), row.label() + seat.number());
            }
        }
        BigDecimal total = BigDecimal.ZERO;
        List<String> seatLabels = new ArrayList<>();
        for (Long seatId : lock.seatIds()) {
            SeatMapSeat seat = seats.get(seatId);
            if (seat == null || seat.price() == null || SeatStatus.BOOKED.equals(seat.status())) {
                throw new ConflictException("Seat " + seatId + " can no longer be sold. Pick again.");
            }
            total = total.add(seat.price());
            seatLabels.add(labels.get(seatId));
        }
        Show show = showRepository
                .findById(lock.showId())
                .orElseThrow(() -> new NotFoundException("Show " + lock.showId() + " not found"));
        return new ActiveLock(
                lock.lockId(),
                lock.userId(),
                lock.showId(),
                lock.seatIds(),
                total,
                show.getCinemaId(),
                map.screenName(),
                map.movieName(),
                map.showDate(),
                map.startTime(),
                List.copyOf(seatLabels),
                show.getMovieId());
    }

    /**
     * Used by the waitlist: holds seats for a user without any user request, for the given time. Returns the lock ID,
     * or empty if a seat is taken or the user already holds seats for this show.
     */
    public Optional<String> lockForUser(long userId, long showId, List<Long> seatIds, Duration ttl) {
        String lockId = UUID.randomUUID().toString();
        LockResult result = seatLockStore.lock(lockId, userId, showId, seatIds, ttl);
        return result == LockResult.LOCKED ? Optional.of(lockId) : Optional.empty();
    }

    /** Used by the booking module once seats are sold. Does nothing if the lock already expired. */
    public void releaseAfterSale(String lockId) {
        seatLockStore.find(lockId).ifPresent(seatLockStore::release);
    }

    private HeldLock ownedLock(String lockId, CurrentUser user) {
        HeldLock lock = seatLockStore
                .find(lockId)
                .orElseThrow(() -> new NotFoundException("Lock " + lockId + " not found or already expired"));
        if (lock.userId() != user.id()) {
            throw new ForbiddenException("You cannot use another user's seats");
        }
        return lock;
    }
}
