package com.bookmyshow.modules.booking;

import org.springframework.data.jpa.repository.JpaRepository;

interface BookingMemberRepository extends JpaRepository<BookingMember, Long> {

    boolean existsByBookingIdAndUserId(Long bookingId, Long userId);
}
