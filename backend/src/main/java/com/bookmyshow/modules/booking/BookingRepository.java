package com.bookmyshow.modules.booking;

import jakarta.persistence.LockModeType;
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

    List<Booking> findByUserIdAndMovieIdAndStatusAndShowDateLessThanEqual(
            Long userId, Long movieId, String status, LocalDate onOrBefore);

    Page<Booking> findByUserIdAndShowDateGreaterThanEqualAndStatusIn(
            Long userId, LocalDate from, Collection<String> statuses, Pageable pageable);

    Page<Booking> findByUserIdAndShowDateLessThanAndStatusIn(
            Long userId, LocalDate before, Collection<String> statuses, Pageable pageable);

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
