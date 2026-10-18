package com.bookmyshow.modules.screen;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ScreenRepository extends JpaRepository<Screen, Long> {

    List<Screen> findAllByCinemaIdOrderByNameAsc(Long cinemaId);
}
