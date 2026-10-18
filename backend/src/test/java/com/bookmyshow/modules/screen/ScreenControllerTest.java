package com.bookmyshow.modules.screen;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.screen.ScreenTypes.LayoutResponse;
import com.bookmyshow.modules.screen.ScreenTypes.RowResponse;
import com.bookmyshow.modules.screen.ScreenTypes.ScreenResponse;
import com.bookmyshow.modules.screen.ScreenTypes.SeatResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ScreenController.class)
class ScreenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScreenService screenService;

    @Test
    void listsScreensInCinema() throws Exception {
        when(screenService.listByCinema(10L)).thenReturn(List.of(new ScreenResponse(1L, 10L, "Screen 1", 116L)));

        mockMvc.perform(get("/api/v1/cinemas/10/screens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Screen 1"))
                .andExpect(jsonPath("$.items[0].seatCount").value(116));
    }

    @Test
    void returns404WhenCinemaMissing() throws Exception {
        when(screenService.listByCinema(99L)).thenThrow(new NotFoundException("Cinema 99 not found"));

        mockMvc.perform(get("/api/v1/cinemas/99/screens"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cinema 99 not found"));
    }

    @Test
    void getsLayout() throws Exception {
        LayoutResponse layout = new LayoutResponse(
                1L,
                10L,
                "Screen 1",
                2,
                List.of(new RowResponse(
                        "A", List.of(new SeatResponse(5L, 1, "REGULAR"), new SeatResponse(6L, 2, "REGULAR")))));
        when(screenService.getLayout(1L)).thenReturn(layout);

        mockMvc.perform(get("/api/v1/screens/1/layout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatCount").value(2))
                .andExpect(jsonPath("$.rows[0].label").value("A"))
                .andExpect(jsonPath("$.rows[0].seats[1].number").value(2))
                .andExpect(jsonPath("$.rows[0].seats[1].category").value("REGULAR"));
    }

    @Test
    void returns404WhenScreenMissing() throws Exception {
        when(screenService.getLayout(99L)).thenThrow(new NotFoundException("Screen 99 not found"));

        mockMvc.perform(get("/api/v1/screens/99/layout"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Screen 99 not found"));
    }

    @Test
    void returns400ForZeroId() throws Exception {
        mockMvc.perform(get("/api/v1/screens/0/layout")).andExpect(status().isBadRequest());
    }
}
