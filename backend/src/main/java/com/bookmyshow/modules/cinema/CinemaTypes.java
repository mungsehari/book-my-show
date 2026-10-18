package com.bookmyshow.modules.cinema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** All request and response types for the cinema module. */
public final class CinemaTypes {

    private CinemaTypes() {}

    public record CinemaResponse(Long id, Long cityId, String name, String address) {

        static CinemaResponse from(Cinema cinema) {
            return new CinemaResponse(cinema.getId(), cinema.getCityId(), cinema.getName(), cinema.getAddress());
        }
    }

    public record AssignAdminRequest(@NotNull @Positive Long userId) {}

    public record CinemaAdminResponse(Long cinemaId, Long userId) {

        static CinemaAdminResponse from(CinemaAdmin admin) {
            return new CinemaAdminResponse(admin.getCinemaId(), admin.getUserId());
        }
    }
}
