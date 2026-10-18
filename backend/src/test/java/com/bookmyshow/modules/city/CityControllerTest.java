package com.bookmyshow.modules.city;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.modules.city.CityTypes.CityResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithSecurityConfig
@WebMvcTest(CityController.class)
class CityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CityService cityService;

    @Test
    void listsCities() throws Exception {
        when(cityService.listCities())
                .thenReturn(List.of(
                        new CityResponse(1L, "Mumbai", "Maharashtra"), new CityResponse(2L, "Pune", "Maharashtra")));

        mockMvc.perform(get("/api/v1/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.items[0].name").value("Mumbai"))
                .andExpect(jsonPath("$.items[1].state").value("Maharashtra"));
    }

    @Test
    void returnsEmptyList() throws Exception {
        when(cityService.listCities()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }
}
