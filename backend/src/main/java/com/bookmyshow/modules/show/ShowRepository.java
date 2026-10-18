package com.bookmyshow.modules.show;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findAllByCinemaIdAndShowDateOrderByStartTimeAsc(Long cinemaId, LocalDate showDate);

    List<Show> findAllByScreenIdAndShowDate(Long screenId, LocalDate showDate);

    /** Movies with at least one show at these cinemas between the two dates, inclusive. Unlinked shows are skipped. */
    @Query("""
            select distinct s.movieId from Show s
            where s.cinemaId in :cinemaIds
              and s.showDate between :from and :to
              and s.movieId is not null
            """)
    List<Long> findMovieIdsPlaying(Collection<Long> cinemaIds, LocalDate from, LocalDate to);
}
