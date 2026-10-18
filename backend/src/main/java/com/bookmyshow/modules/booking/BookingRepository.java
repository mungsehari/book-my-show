package com.bookmyshow.modules.booking;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByLockId(String lockId);

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
