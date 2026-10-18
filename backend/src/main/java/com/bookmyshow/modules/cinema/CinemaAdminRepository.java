package com.bookmyshow.modules.cinema;

import org.springframework.data.jpa.repository.JpaRepository;

interface CinemaAdminRepository extends JpaRepository<CinemaAdmin, Long> {

    boolean existsByUserIdAndCinemaId(Long userId, Long cinemaId);
}
