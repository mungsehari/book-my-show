package com.bookmyshow.modules.show;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Every query here looks at active sales only (releasedAt null). Released rows are history. */
interface ShowBookedSeatRepository extends JpaRepository<ShowBookedSeat, Long> {

    @Query("select b.seatId from ShowBookedSeat b where b.showId = :showId and b.releasedAt is null")
    List<Long> findSeatIdsByShowId(Long showId);

    boolean existsByShowIdAndSeatIdInAndReleasedAtIsNull(Long showId, Collection<Long> seatIds);

    List<ShowBookedSeat> findByBookingIdAndReleasedAtIsNull(Long bookingId);
}
