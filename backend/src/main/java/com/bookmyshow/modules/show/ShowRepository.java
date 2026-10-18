package com.bookmyshow.modules.show;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findAllByCinemaIdAndShowDateOrderByStartTimeAsc(Long cinemaId, LocalDate showDate);

    List<Show> findAllByScreenIdAndShowDate(Long screenId, LocalDate showDate);

    List<Show> findByCinemaIdInAndShowDateOrderByStartTimeAsc(Collection<Long> cinemaIds, LocalDate showDate);

    List<Show> findByCinemaIdInAndMovieIdAndShowDateBetweenOrderByShowDateAscStartTimeAsc(
            Collection<Long> cinemaIds, Long movieId, LocalDate from, LocalDate to);

    /** Locks the show row until the transaction ends, so seat sales for one show run one at a time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Show s where s.id = :id")
    Optional<Show> findByIdForUpdate(Long id);

    /** Movies with at least one show at these cinemas between the two dates, inclusive. Unlinked shows are skipped. */
    @Query("""
            select distinct s.movieId from Show s
            where s.cinemaId in :cinemaIds
              and s.showDate between :from and :to
              and s.movieId is not null
            """)
    List<Long> findMovieIdsPlaying(Collection<Long> cinemaIds, LocalDate from, LocalDate to);
}
