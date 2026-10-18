package com.bookmyshow.modules.city;

/** All request and response types for the city module. */
public final class CityTypes {

    private CityTypes() {}

    public record CityResponse(Long id, String name, String state) {

        static CityResponse from(City city) {
            return new CityResponse(city.getId(), city.getName(), city.getState());
        }
    }
}
