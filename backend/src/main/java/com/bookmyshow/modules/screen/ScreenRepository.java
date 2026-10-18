package com.bookmyshow.modules.screen;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface ScreenRepository extends JpaRepository<Screen, Long> {

    List<Screen> findAllByCinemaIdOrderByNameAsc(Long cinemaId);

    boolean existsByCinemaIdAndNameIgnoreCase(Long cinemaId, String name);

    /** Locks the screen row until the transaction ends (SELECT ... FOR UPDATE). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Screen s where s.id = :id")
    Optional<Screen> findByIdForUpdate(Long id);
}
