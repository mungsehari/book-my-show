package com.bookmyshow.modules.city;

import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.city.CityTypes.CityResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CityService {

    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public List<CityResponse> listCities() {
        return cityRepository.findAllByOrderByNameAsc().stream()
                .map(CityResponse::from)
                .toList();
    }

    /** Used by other modules to check a city before using its ID. */
    public void ensureExists(long cityId) {
        if (!cityRepository.existsById(cityId)) {
            throw new NotFoundException("City " + cityId + " not found");
        }
    }
}
