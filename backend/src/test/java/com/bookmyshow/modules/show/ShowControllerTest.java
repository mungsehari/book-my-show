package com.bookmyshow.modules.show;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.common.error.NotFoundException;
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

@WebMvcTest(ShowController.class)
class ShowControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 17);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShowService showService;

    @Test
    void listsShowsForDate() throws Exception {
        when(showService.listByCinema(10L, DATE))
                .thenReturn(List.of(new ShowResponse(
                        100L, 10L, "The Last Monsoon", DATE, LocalTime.of(18, 30), new BigDecimal("320.00"))));

        mockMvc.perform(get("/api/v1/cinemas/10/shows").param("date", "2026-10-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
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
}
