package com.bookmyshow.modules.screen;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/** All request and response types for the screen module. */
public final class ScreenTypes {

    private ScreenTypes() {}

    /** A new screen and its full layout. Each row has seats numbered 1 to seatCount. */
    public record CreateScreenRequest(
            @NotBlank @Size(max = 50) String name,
            @NotEmpty @Size(max = 26) List<@Valid @NotNull RowRequest> rows) {}

    /** wheelchairSeats lists seat numbers in this row with space for a wheelchair. Optional. */
    public record RowRequest(
            @NotBlank @Pattern(regexp = "^[A-Z]$") String label,
            @NotNull @Positive @Max(50) Integer seatCount,

            @NotNull @Pattern(regexp = "^(REGULAR|PREMIUM|RECLINER)$")
            String category,

            @Size(max = 50) List<@NotNull @Positive Integer> wheelchairSeats) {}

    public record ScreenResponse(Long id, Long cinemaId, String name, long seatCount) {

        static ScreenResponse from(Screen screen, long seatCount) {
            return new ScreenResponse(screen.getId(), screen.getCinemaId(), screen.getName(), seatCount);
        }
    }

    public record SeatResponse(Long id, Integer number, String category, boolean wheelchair) {

        static SeatResponse from(Seat seat) {
            return new SeatResponse(seat.getId(), seat.getSeatNumber(), seat.getCategory(), seat.isWheelchair());
        }
    }

    public record RowResponse(String label, List<SeatResponse> seats) {}

    public record LayoutResponse(Long screenId, Long cinemaId, String name, long seatCount, List<RowResponse> rows) {}
}
