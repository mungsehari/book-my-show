package com.bookmyshow.modules.cinema;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.cinema.CinemaTypes.CinemaResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CinemaController.class)
class CinemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CinemaService cinemaService;

    @Test
    void listsCinemasInCity() throws Exception {
        when(cinemaService.listByCity(1L))
                .thenReturn(List.of(new CinemaResponse(10L, 1L, "City Cineplex Andheri", "Andheri West, Mumbai")));

        mockMvc.perform(get("/api/v1/cities/1/cinemas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.items[0].name").value("City Cineplex Andheri"))
                .andExpect(jsonPath("$.items[0].cityId").value(1));
    }

    @Test
    void returns404WhenCityMissing() throws Exception {
        when(cinemaService.listByCity(99L)).thenThrow(new NotFoundException("City 99 not found"));

        mockMvc.perform(get("/api/v1/cities/99/cinemas"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("City 99 not found"));
    }

    @Test
    void returns400ForNonNumericId() throws Exception {
        mockMvc.perform(get("/api/v1/cities/abc/cinemas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for 'cityId'"));
    }

    @Test
    void returns400ForZeroId() throws Exception {
        mockMvc.perform(get("/api/v1/cities/0/cinemas")).andExpect(status().isBadRequest());
    }
}
