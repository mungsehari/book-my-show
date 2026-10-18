package com.bookmyshow.modules.show;

import com.bookmyshow.modules.cinema.CinemaService;
import com.bookmyshow.modules.show.ShowTypes.ShowResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ShowService {

    private final ShowRepository showRepository;
    private final CinemaService cinemaService;
    private final Clock clock;

    public ShowService(ShowRepository showRepository, CinemaService cinemaService, Clock clock) {
        this.showRepository = showRepository;
        this.cinemaService = cinemaService;
        this.clock = clock;
    }

    /** Lists shows for a cinema on a date. No date means today in the app time zone. */
    public List<ShowResponse> listByCinema(long cinemaId, LocalDate date) {
        cinemaService.ensureExists(cinemaId);
        LocalDate showDate = date != null ? date : LocalDate.now(clock);
        return showRepository.findAllByCinemaIdAndShowDateOrderByStartTimeAsc(cinemaId, showDate).stream()
                .map(ShowResponse::from)
                .toList();
    }
}
