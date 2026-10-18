package com.bookmyshow.modules.booking;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface PaymentShareRepository extends JpaRepository<PaymentShare, Long> {

    List<PaymentShare> findByBookingIdOrderByIdAsc(Long bookingId);

    List<PaymentShare> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    /** Locks the share row, so the confirm call and the webhook cannot both apply one payment. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PaymentShare s where s.id = :id")
    Optional<PaymentShare> findByIdForUpdate(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PaymentShare s where s.razorpayOrderId = :orderId")
    Optional<PaymentShare> findByRazorpayOrderIdForUpdate(String orderId);
}
