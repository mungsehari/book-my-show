package com.bookmyshow.modules.booking;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByLockId(String lockId);

    List<Booking> findByMovieIdAndStatusAndUserIdIn(Long movieId, String status, Collection<Long> userIds);

    /** Unpaid one-payer checkouts older than the cut-off, for the expiry job. */
    List<Booking> findTop100ByStatusAndSplitFalseAndCreatedAtBefore(String status, Instant before);

    /** Unpaid split checkouts older than the cut-off, for the split expiry job. */
    List<Booking> findTop100ByStatusAndSplitTrueAndCreatedAtBefore(String status, Instant before);

    /** Resale tickets on offer for a show. */
    List<Booking> findByShowIdAndStatusAndResaleListedAtIsNotNullOrderByResaleListedAtAsc(Long showId, String status);

    /** A buyer's unpaid resale checkout for a seller's ticket, newer than the cut-off. */
    Optional<Booking> findFirstByResaleOfBookingIdAndStatusAndCreatedAtAfter(
            Long sellerBookingId, String status, Instant after);

    List<Booking> findByUserIdAndMovieIdAndStatusAndShowDateLessThanEqual(
            Long userId, Long movieId, String status, LocalDate onOrBefore);

    Page<Booking> findByUserIdAndShowDateGreaterThanEqualAndStatusIn(
            Long userId, LocalDate from, Collection<String> statuses, Pageable pageable);

    Page<Booking> findByUserIdAndShowDateLessThanAndStatusIn(
            Long userId, LocalDate before, Collection<String> statuses, Pageable pageable);

    /** Admin stats: sales confirmed since a moment, in the given cinemas. */
    List<Booking> findByStatusAndConfirmedAtGreaterThanEqualAndCinemaIdIn(
            String status, Instant from, Collection<Long> cinemaIds);

    long countByStatusAndCinemaIdIn(String status, Collection<Long> cinemaIds);

    long countByStatusAndCreatedAtGreaterThanEqualAndCinemaIdIn(
            String status, Instant from, Collection<Long> cinemaIds);

    Page<Booking> findByCinemaIdIn(Collection<Long> cinemaIds, Pageable pageable);

    Page<Booking> findByStatusAndCinemaIdIn(String status, Collection<Long> cinemaIds, Pageable pageable);

    /**
     * Locks the booking row until the transaction ends, so the confirm call and the webhook cannot both apply the
     * same payment.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.razorpayOrderId = :orderId")
    Optional<Booking> findByRazorpayOrderIdForUpdate(String orderId);
}
