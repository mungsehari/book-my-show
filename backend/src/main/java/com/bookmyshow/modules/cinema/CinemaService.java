package com.bookmyshow.modules.cinema;

import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.cinema.CinemaTypes.CinemaResponse;
import com.bookmyshow.modules.city.CityService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CinemaService {

    private final CinemaRepository cinemaRepository;
    private final CityService cityService;

    public CinemaService(CinemaRepository cinemaRepository, CityService cityService) {
        this.cinemaRepository = cinemaRepository;
        this.cityService = cityService;
    }

    public List<CinemaResponse> listByCity(long cityId) {
        cityService.ensureExists(cityId);
        return cinemaRepository.findAllByCityIdOrderByNameAsc(cityId).stream()
                .map(CinemaResponse::from)
                .toList();
    }

    /** Used by other modules to check a cinema before using its ID. */
    public void ensureExists(long cinemaId) {
        if (!cinemaRepository.existsById(cinemaId)) {
            throw new NotFoundException("Cinema " + cinemaId + " not found");
        }
    }
}
