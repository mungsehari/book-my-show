package com.bookmyshow.modules.cinema;

/** All request and response types for the cinema module. */
public final class CinemaTypes {

    private CinemaTypes() {}

    public record CinemaResponse(Long id, Long cityId, String name, String address) {

        static CinemaResponse from(Cinema cinema) {
            return new CinemaResponse(cinema.getId(), cinema.getCityId(), cinema.getName(), cinema.getAddress());
        }
    }
}
