package com.bookmyshow.modules.cinema;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface CinemaRepository extends JpaRepository<Cinema, Long> {

    List<Cinema> findAllByCityIdOrderByNameAsc(Long cityId);
}
