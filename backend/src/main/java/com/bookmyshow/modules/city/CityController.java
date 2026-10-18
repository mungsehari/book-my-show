package com.bookmyshow.modules.city;

import com.bookmyshow.common.response.ListResponse;
import com.bookmyshow.modules.city.CityTypes.CityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cities")
@Tag(name = "Cities")
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping
    @Operation(summary = "List all cities")
    public ListResponse<CityResponse> listCities() {
        return ListResponse.of(cityService.listCities());
    }
}
