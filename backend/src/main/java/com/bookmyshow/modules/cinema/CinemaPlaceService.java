package com.bookmyshow.modules.cinema;

import com.bookmyshow.modules.cinema.CinemaTypes.CreatePlaceRequest;
import com.bookmyshow.modules.cinema.CinemaTypes.PlaceResponse;
import com.bookmyshow.security.CurrentUser;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Food and parking near a cinema, for the weekend planner. Theatres keep this list up to date. */
@Service
@Transactional(readOnly = true)
public class CinemaPlaceService {

    private final CinemaPlaceRepository placeRepository;
    private final CinemaService cinemaService;
    private final CinemaAccessService cinemaAccessService;
    private final Clock clock;

    public CinemaPlaceService(
            CinemaPlaceRepository placeRepository,
            CinemaService cinemaService,
            CinemaAccessService cinemaAccessService,
            Clock clock) {
        this.placeRepository = placeRepository;
        this.cinemaService = cinemaService;
        this.cinemaAccessService = cinemaAccessService;
        this.clock = clock;
    }

    /** Nearest first. */
    public List<PlaceResponse> list(long cinemaId) {
        cinemaService.ensureExists(cinemaId);
        return placeRepository.findByCinemaIdOrderByWalkMinutesAscNameAsc(cinemaId).stream()
                .map(PlaceResponse::from)
                .toList();
    }

    @Transactional
    public PlaceResponse add(long cinemaId, CreatePlaceRequest request, CurrentUser user) {
        cinemaAccessService.ensureCanManage(user, cinemaId);
        String notes = request.notes() == null || request.notes().isBlank()
                ? null
                : request.notes().strip();
        CinemaPlace place = placeRepository.save(new CinemaPlace(
                cinemaId, request.type(), request.name().strip(), request.walkMinutes(), notes, clock.instant()));
        return PlaceResponse.from(place);
    }
}
