package com.bookmyshow.modules.waitlist;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.modules.show.ShowCrowdService;
import com.bookmyshow.modules.show.ShowSeatLockService;
import com.bookmyshow.modules.show.ShowTypes.CrowdResponse;
import com.bookmyshow.modules.waitlist.WaitlistTypes.WaitlistResponse;
import com.bookmyshow.security.CurrentUser;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Waitlist for full shows. When seats free up (a cancel, or an offer nobody took), the first person in the queue who
 * fits gets them held for {@link #OFFER_TTL}, and is told the lock ID to check out with.
 */
@Service
@Transactional(readOnly = true)
public class WaitlistService {

    static final Duration OFFER_TTL = Duration.ofMinutes(10);

    private final WaitlistRepository waitlistRepository;
    private final ShowCrowdService crowdService;
    private final ShowSeatLockService seatLockService;
    private final Notifier notifier;
    private final Clock clock;

    public WaitlistService(
            WaitlistRepository waitlistRepository,
            ShowCrowdService crowdService,
            ShowSeatLockService seatLockService,
            Notifier notifier,
            Clock clock) {
        this.waitlistRepository = waitlistRepository;
        this.crowdService = crowdService;
        this.seatLockService = seatLockService;
        this.notifier = notifier;
        this.clock = clock;
    }

    /** Joins the queue. Only when the show does not have enough free seats right now. */
    @Transactional
    public WaitlistResponse join(long showId, int seats, CurrentUser user) {
        CrowdResponse crowd = crowdService.getCrowd(showId);
        if (crowd.available() >= seats) {
            throw new BadRequestException(
                    crowd.available() + " seats are free for show " + showId + ". Book them directly.");
        }
        if (waitlistRepository.existsByShowIdAndUserIdAndStatusIn(showId, user.id(), WaitlistStatus.ACTIVE)) {
            throw new ConflictException("You are already on the waitlist for show " + showId);
        }
        WaitlistEntry entry = waitlistRepository.save(new WaitlistEntry(showId, user.id(), seats, clock.instant()));
        return toResponse(entry);
    }

    public WaitlistResponse myEntry(long showId, CurrentUser user) {
        return waitlistRepository
                .findFirstByShowIdAndUserIdAndStatusIn(showId, user.id(), WaitlistStatus.ACTIVE)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("You are not on the waitlist for show " + showId));
    }

    /** Leaves the queue. Seats held for the user go to the next person. */
    @Transactional
    public void leave(long entryId, CurrentUser user) {
        WaitlistEntry entry = waitlistRepository
                .findById(entryId)
                .orElseThrow(() -> new NotFoundException("Waitlist entry " + entryId + " not found"));
        if (entry.getUserId() != user.id()) {
            throw new ForbiddenException("This waitlist entry belongs to another user");
        }
        if (!entry.isActive()) {
            return;
        }
        boolean wasOffered = WaitlistStatus.OFFERED.equals(entry.getStatus());
        entry.leave();
        if (wasOffered) {
            seatLockService.releaseAfterSale(entry.getLockId());
            offerSeats(entry.getShowId(), entry.getOfferedSeatIds());
        }
    }

    /**
     * Used by the booking module when seats are freed. Offers them to the queue in order. A person who wants more seats
     * than are free is skipped, not blocked: the next person who fits gets them.
     */
    @Transactional
    public void offerSeats(long showId, List<Long> freedSeatIds) {
        List<Long> remaining = new ArrayList<>(freedSeatIds);
        for (WaitlistEntry entry :
                waitlistRepository.findByShowIdAndStatusOrderByCreatedAtAsc(showId, WaitlistStatus.WAITING)) {
            if (remaining.isEmpty()) {
                return;
            }
            if (entry.getSeatsWanted() > remaining.size()) {
                continue;
            }
            List<Long> chosen = List.copyOf(remaining.subList(0, entry.getSeatsWanted()));
            Optional<String> lockId = seatLockService.lockForUser(entry.getUserId(), showId, chosen, OFFER_TTL);
            if (lockId.isEmpty()) {
                continue;
            }
            Instant now = clock.instant();
            entry.offer(lockId.get(), chosen, now, now.plus(OFFER_TTL));
            remaining.removeAll(chosen);
            notifier.notify(
                    entry.getUserId(),
                    "Seats are waiting for you",
                    chosen.size() + " seats for show " + showId + " are held for you for " + OFFER_TTL.toMinutes()
                            + " minutes. Check out with lock " + lockId.get() + ".");
        }
    }

    /** Used by the booking module when a waitlist offer was paid. */
    @Transactional
    public void markBooked(String lockId) {
        waitlistRepository.findByLockIdAndStatus(lockId, WaitlistStatus.OFFERED).ifPresent(WaitlistEntry::markBooked);
    }

    /** Run by a job: offers nobody took in time go to the next person. Returns how many expired. */
    @Transactional
    public int expireOffers() {
        List<WaitlistEntry> expired =
                waitlistRepository.findTop100ByStatusAndOfferExpiresAtBefore(WaitlistStatus.OFFERED, clock.instant());
        for (WaitlistEntry entry : expired) {
            entry.markExpired();
            seatLockService.releaseAfterSale(entry.getLockId());
            offerSeats(entry.getShowId(), entry.getOfferedSeatIds());
        }
        return expired.size();
    }

    private WaitlistResponse toResponse(WaitlistEntry entry) {
        Long position = WaitlistStatus.WAITING.equals(entry.getStatus())
                ? waitlistRepository.countByShowIdAndStatusAndCreatedAtBefore(
                                entry.getShowId(), WaitlistStatus.WAITING, entry.getCreatedAt())
                        + 1
                : null;
        return new WaitlistResponse(
                entry.getId(),
                entry.getShowId(),
                entry.getSeatsWanted(),
                entry.getStatus(),
                position,
                entry.getLockId(),
                entry.getOfferedSeatIds(),
                entry.getOfferExpiresAt());
    }
}
