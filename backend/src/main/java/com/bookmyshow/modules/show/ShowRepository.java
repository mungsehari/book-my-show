package com.bookmyshow.modules.show;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findAllByCinemaIdAndShowDateOrderByStartTimeAsc(Long cinemaId, LocalDate showDate);
}
