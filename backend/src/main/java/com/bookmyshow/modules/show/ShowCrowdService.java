package com.bookmyshow.modules.show;

import com.bookmyshow.modules.show.ShowTypes.CrowdResponse;
import com.bookmyshow.modules.show.ShowTypes.SeatMapSeat;
import com.bookmyshow.modules.show.ShowTypes.SeatStatus;
import java.util.List;
import org.springframework.stereotype.Service;

/** Crowd meter: how full a show is, from live seat states (sold in the database, held in Redis). */
@Service
public class ShowCrowdService {

    private final ShowSeatMapService seatMapService;

    public ShowCrowdService(ShowSeatMapService seatMapService) {
        this.seatMapService = seatMapService;
    }

    public CrowdResponse getCrowd(long showId) {
        List<SeatMapSeat> seats = seatMapService.getSeatMap(showId).rows().stream()
                .flatMap(row -> row.seats().stream())
                .filter(seat -> !SeatStatus.UNAVAILABLE.equals(seat.status()))
                .toList();
        int total = seats.size();
        int sold = count(seats, SeatStatus.BOOKED);
        int held = count(seats, SeatStatus.LOCKED);
        int percent = total == 0 ? 0 : (sold + held) * 100 / total;
        return new CrowdResponse(showId, total, sold, held, total - sold - held, percent, level(percent, total));
    }

    static String level(int percentFull, int totalSeats) {
        if (totalSeats > 0 && percentFull >= 100) {
            return "HOUSEFULL";
        }
        if (percentFull >= 75) {
            return "HIGH";
        }
        return percentFull >= 40 ? "MEDIUM" : "LOW";
    }

    private static int count(List<SeatMapSeat> seats, String status) {
        return (int) seats.stream().filter(seat -> status.equals(seat.status())).count();
    }
}
