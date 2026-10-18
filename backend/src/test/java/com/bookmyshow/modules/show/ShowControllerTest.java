package com.bookmyshow.modules.show;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
import com.bookmyshow.modules.show.ShowTypes.CategoryPrice;
import com.bookmyshow.modules.show.ShowTypes.SeatMapResponse;
import com.bookmyshow.modules.show.ShowTypes.SeatMapRow;
import com.bookmyshow.modules.show.ShowTypes.SeatMapSeat;
import com.bookmyshow.modules.show.ShowTypes.SeatStatus;
import com.bookmyshow.modules.show.ShowTypes.ShowResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithSecurityConfig
@WebMvcTest(ShowController.class)
class ShowControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 17);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShowService showService;

    @MockitoBean
    private ShowSeatMapService showSeatMapService;

    @Test
    void listsShowsForDate() throws Exception {
        when(showService.listByCinema(10L, DATE))
                .thenReturn(List.of(new ShowResponse(
                        100L,
                        10L,
                        null,
                        5L,
                        "The Last Monsoon",
                        DATE,
                        LocalTime.of(18, 30),
                        new BigDecimal("320.00"))));

        mockMvc.perform(get("/api/v1/cinemas/10/shows").param("date", "2026-10-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.items[0].movieId").value(5))
                .andExpect(jsonPath("$.items[0].movieName").value("The Last Monsoon"))
                .andExpect(jsonPath("$.items[0].showDate").value("2026-10-17"))
                .andExpect(jsonPath("$.items[0].startTime").value("18:30:00"));
    }

    @Test
    void passesNullDateWhenMissing() throws Exception {
        when(showService.listByCinema(eq(10L), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/cinemas/10/shows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void returns400ForBadDate() throws Exception {
        mockMvc.perform(get("/api/v1/cinemas/10/shows").param("date", "2026-13-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for 'date'"));
    }

    @Test
    void returns404WhenCinemaMissing() throws Exception {
        when(showService.listByCinema(99L, DATE)).thenThrow(new NotFoundException("Cinema 99 not found"));

        mockMvc.perform(get("/api/v1/cinemas/99/shows").param("date", "2026-10-17"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cinema 99 not found"));
    }

    @Test
    void listsMoviesInCity() throws Exception {
        when(showService.listMoviesInCity(eq(1L), isNull()))
                .thenReturn(List.of(new MovieResponse(
                        2L, "Sky Riders", "Plot.", "English", "Action", 128, "UA", LocalDate.of(2026, 10, 2), null)));

        mockMvc.perform(get("/api/v1/cities/1/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Sky Riders"));
    }

    @Test
    void returns400ForBadDateOnCityMovies() throws Exception {
        mockMvc.perform(get("/api/v1/cities/1/movies").param("date", "2026-13-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for 'date'"));
    }

    @Test
    void returns404WhenCityMissingOnCityMovies() throws Exception {
        when(showService.listMoviesInCity(99L, DATE)).thenThrow(new NotFoundException("City 99 not found"));

        mockMvc.perform(get("/api/v1/cities/99/movies").param("date", "2026-10-17"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("City 99 not found"));
    }

    @Test
    void getsSeatMapWithoutLogin() throws Exception {
        when(showSeatMapService.getSeatMap(50L))
                .thenReturn(new SeatMapResponse(
                        50L,
                        2L,
                        "Sky Riders",
                        DATE,
                        LocalTime.of(14, 0),
                        3L,
                        "Screen 1",
                        List.of(new CategoryPrice("REGULAR", new BigDecimal("200.00"))),
                        List.of(new SeatMapRow(
                                "A",
                                List.of(new SeatMapSeat(
                                        1L, 1, "REGULAR", new BigDecimal("200.00"), SeatStatus.AVAILABLE))))));

        mockMvc.perform(get("/api/v1/shows/50/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screenName").value("Screen 1"))
                .andExpect(jsonPath("$.rows[0].seats[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.prices[0].price").value(200.00));
    }

    @Test
    void returns404WhenShowHasNoSeatMap() throws Exception {
        when(showSeatMapService.getSeatMap(99L)).thenThrow(new NotFoundException("Show 99 not found"));

        mockMvc.perform(get("/api/v1/shows/99/seats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Show 99 not found"));
    }
}
