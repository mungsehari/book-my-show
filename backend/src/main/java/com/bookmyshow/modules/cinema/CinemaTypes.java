package com.bookmyshow.modules.cinema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** All request and response types for the cinema module. */
public final class CinemaTypes {

    private CinemaTypes() {}

    public record CinemaResponse(Long id, Long cityId, String name, String address) {

        static CinemaResponse from(Cinema cinema) {
            return new CinemaResponse(cinema.getId(), cinema.getCityId(), cinema.getName(), cinema.getAddress());
        }
    }

    public record AssignAdminRequest(@NotNull @Positive Long userId) {}

    /** type is FOOD or PARKING. walkMinutes is the walk from the cinema (0 to 60). */
    public record CreatePlaceRequest(
            @NotNull @Pattern(regexp = "^(FOOD|PARKING)$") String type,
            @NotBlank @Size(max = 150) String name,
            @NotNull @Min(0) @Max(60) Integer walkMinutes,
            @Size(max = 300) String notes) {}

    public record PlaceResponse(Long id, Long cinemaId, String type, String name, Integer walkMinutes, String notes) {

        static PlaceResponse from(CinemaPlace place) {
            return new PlaceResponse(
                    place.getId(),
                    place.getCinemaId(),
                    place.getType(),
                    place.getName(),
                    place.getWalkMinutes(),
                    place.getNotes());
        }
    }

    public record CinemaAdminResponse(Long cinemaId, Long userId) {

        static CinemaAdminResponse from(CinemaAdmin admin) {
            return new CinemaAdminResponse(admin.getCinemaId(), admin.getUserId());
        }
    }
}
