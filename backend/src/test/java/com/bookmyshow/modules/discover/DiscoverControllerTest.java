package com.bookmyshow.modules.discover;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.TestLogins;
import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.integrations.ai.MovieFilters;
import com.bookmyshow.modules.discover.DiscoverTypes.MoodSearchResponse;
import com.bookmyshow.security.Roles;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithSecurityConfig
@WebMvcTest(DiscoverController.class)
class DiscoverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MoodSearchService moodSearchService;

    @MockitoBean
    private PlannerService plannerService;

    @Test
    void moodSearchIsPublic() throws Exception {
        when(moodSearchService.search(eq("kuch halka funny"), isNull(), anyString()))
                .thenReturn(new MoodSearchResponse("kuch halka funny", MovieFilters.NONE, "keyword", List.of()));

        mockMvc.perform(get("/api/v1/movies/search").param("q", "kuch halka funny"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("keyword"));
    }

    @Test
    void moodSearchNeedsText() throws Exception {
        mockMvc.perform(get("/api/v1/movies/search").param("q", " ")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/movies/search").param("q", "x".repeat(201)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void plannerNeedsLogin() throws Exception {
        mockMvc.perform(post("/api/v1/planner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityId\": 1, \"date\": \"2026-10-24\", \"people\": 2, \"time\": \"EVENING\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void plannerRejectsUnknownTimeSlot() throws Exception {
        mockMvc.perform(post("/api/v1/planner")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityId\": 1, \"date\": \"2026-10-24\", \"people\": 2, \"time\": \"NIGHT\"}"))
                .andExpect(status().isBadRequest());
    }
}
