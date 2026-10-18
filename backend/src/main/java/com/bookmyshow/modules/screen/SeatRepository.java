package com.bookmyshow.modules.screen;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findAllByScreenIdOrderByRowLabelAscSeatNumberAsc(Long screenId);

    @Query("select distinct s.category from Seat s where s.screenId = :screenId")
    List<String> findCategoriesByScreenId(Long screenId);

    @Query("select distinct s.screenId from Seat s where s.screenId in :screenIds and s.wheelchair = true")
    List<Long> findScreenIdsWithWheelchairSeats(Collection<Long> screenIds);

    /** Seat count per screen, in one query. Screens with no seats are left out. */
    @Query("""
            select s.screenId as screenId, count(s) as seatCount
            from Seat s
            where s.screenId in :screenIds
            group by s.screenId
            """)
    List<SeatCount> countByScreenIds(Collection<Long> screenIds);

    /** Spring Data fills this from the query aliases. */
    interface SeatCount {

        Long getScreenId();

        Long getSeatCount();
    }
}
