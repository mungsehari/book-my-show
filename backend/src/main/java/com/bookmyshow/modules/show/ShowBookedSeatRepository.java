package com.bookmyshow.modules.show;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ShowBookedSeatRepository extends JpaRepository<ShowBookedSeat, Long> {

    @Query("select b.seatId from ShowBookedSeat b where b.showId = :showId")
    List<Long> findSeatIdsByShowId(Long showId);

    boolean existsByShowIdAndSeatIdIn(Long showId, Collection<Long> seatIds);
}
