package com.bookmyshow.modules.waitlist;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    boolean existsByShowIdAndUserIdAndStatusIn(Long showId, Long userId, Collection<String> statuses);

    Optional<WaitlistEntry> findFirstByShowIdAndUserIdAndStatusIn(
            Long showId, Long userId, Collection<String> statuses);

    /** The queue for a show, first come first served. */
    List<WaitlistEntry> findByShowIdAndStatusOrderByCreatedAtAsc(Long showId, String status);

    long countByShowIdAndStatusAndCreatedAtBefore(Long showId, String status, Instant before);

    Optional<WaitlistEntry> findByLockIdAndStatus(String lockId, String status);

    List<WaitlistEntry> findTop100ByStatusAndOfferExpiresAtBefore(String status, Instant before);
}
