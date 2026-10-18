package com.bookmyshow.modules.cinema;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface CinemaPlaceRepository extends JpaRepository<CinemaPlace, Long> {

    /** Nearest first. */
    List<CinemaPlace> findByCinemaIdOrderByWalkMinutesAscNameAsc(Long cinemaId);
}
