package com.bookmyshow.modules.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookmyshow.modules.show.ShowTypes.CrowdResponse;
import com.bookmyshow.modules.show.ShowTypes.SeatMapResponse;
import com.bookmyshow.modules.show.ShowTypes.SeatMapRow;
import com.bookmyshow.modules.show.ShowTypes.SeatMapSeat;
import com.bookmyshow.modules.show.ShowTypes.SeatStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShowCrowdServiceTest {

    @Test
    void countsSoldAndHeldSeatsForSaleOnly() {
        ShowSeatMapService seatMapService = mock(ShowSeatMapService.class);
        when(seatMapService.getSeatMap(50L))
                .thenReturn(map(
                        seat(1L, SeatStatus.BOOKED),
                        seat(2L, SeatStatus.LOCKED),
                        seat(3L, SeatStatus.AVAILABLE),
                        seat(4L, SeatStatus.AVAILABLE),
                        seat(5L, SeatStatus.UNAVAILABLE)));

        CrowdResponse crowd = new ShowCrowdService(seatMapService).getCrowd(50L);

        assertThat(crowd.totalSeats()).isEqualTo(4);
        assertThat(crowd.sold()).isEqualTo(1);
        assertThat(crowd.held()).isEqualTo(1);
        assertThat(crowd.available()).isEqualTo(2);
        assertThat(crowd.percentFull()).isEqualTo(50);
        assertThat(crowd.level()).isEqualTo("MEDIUM");
    }

    @Test
    void levels() {
        assertThat(ShowCrowdService.level(0, 100)).isEqualTo("LOW");
        assertThat(ShowCrowdService.level(39, 100)).isEqualTo("LOW");
        assertThat(ShowCrowdService.level(40, 100)).isEqualTo("MEDIUM");
        assertThat(ShowCrowdService.level(75, 100)).isEqualTo("HIGH");
        assertThat(ShowCrowdService.level(100, 100)).isEqualTo("HOUSEFULL");
        assertThat(ShowCrowdService.level(0, 0)).isEqualTo("LOW");
    }

    private static SeatMapSeat seat(long id, String status) {
        BigDecimal price = SeatStatus.UNAVAILABLE.equals(status) ? null : new BigDecimal("200.00");
        return new SeatMapSeat(id, (int) id, "REGULAR", price, status, false);
    }

    private static SeatMapResponse map(SeatMapSeat... seats) {
        return new SeatMapResponse(
                50L,
                2L,
                "Sky Riders",
                LocalDate.of(2026, 10, 20),
                LocalTime.of(18, 0),
                3L,
                "Screen 1",
                List.of(),
                List.of(new SeatMapRow("A", List.of(seats))),
                null);
    }
}
