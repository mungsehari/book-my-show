package com.bookmyshow.modules.alert;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    long countByUserIdAndStatus(Long userId, String status);

    boolean existsByUserIdAndMovieIdAndCityIdAndStatus(Long userId, Long movieId, Long cityId, String status);

    List<PriceAlert> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Active alerts in ID order, a page at a time, for the job. */
    List<PriceAlert> findByStatusAndIdGreaterThanOrderByIdAsc(String status, Long afterId, Pageable pageable);
}
