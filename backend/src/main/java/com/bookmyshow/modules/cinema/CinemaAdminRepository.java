package com.bookmyshow.modules.cinema;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface CinemaAdminRepository extends JpaRepository<CinemaAdmin, Long> {

    List<CinemaAdmin> findByUserId(Long userId);

    boolean existsByUserIdAndCinemaId(Long userId, Long cinemaId);
}
