package com.bookmyshow.modules.screen;

import java.util.List;

/** All request and response types for the screen module. */
public final class ScreenTypes {

    private ScreenTypes() {}

    public record ScreenResponse(Long id, Long cinemaId, String name, long seatCount) {

        static ScreenResponse from(Screen screen, long seatCount) {
            return new ScreenResponse(screen.getId(), screen.getCinemaId(), screen.getName(), seatCount);
        }
    }

    public record SeatResponse(Long id, Integer number, String category) {

        static SeatResponse from(Seat seat) {
            return new SeatResponse(seat.getId(), seat.getSeatNumber(), seat.getCategory());
        }
    }

    public record RowResponse(String label, List<SeatResponse> seats) {}

    public record LayoutResponse(Long screenId, Long cinemaId, String name, long seatCount, List<RowResponse> rows) {}
}
