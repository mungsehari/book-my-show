package com.bookmyshow.modules.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByMovieIdAndUserId(Long movieId, Long userId);

    Page<Review> findByMovieId(Long movieId, Pageable pageable);

    /** Average rating and count for a movie, in one query. Average is null when there are no reviews. */
    @Query("select avg(r.rating) as average, count(r) as count from Review r where r.movieId = :movieId")
    RatingSummary summarize(Long movieId);

    /** Spring Data fills this from the query aliases. */
    interface RatingSummary {

        Double getAverage();

        Long getCount();
    }
}
