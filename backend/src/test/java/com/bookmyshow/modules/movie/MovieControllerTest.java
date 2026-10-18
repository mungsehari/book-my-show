package com.bookmyshow.modules.movie;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithSecurityConfig
@WebMvcTest(MovieController.class)
class MovieControllerTest {

    private static final MovieResponse SKY_RIDERS = new MovieResponse(
            1L,
            "Sky Riders",
            "Two rival pilots race across the Himalayas.",
            "English",
            "Action",
            128,
            "UA",
            LocalDate.of(2026, 10, 2),
            null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @Test
    void listsMovies() throws Exception {
        when(movieService.listMovies(null)).thenReturn(List.of(SKY_RIDERS));

        mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Sky Riders"))
                .andExpect(jsonPath("$.items[0].durationMinutes").value(128));
    }

    @Test
    void passesLanguageFilter() throws Exception {
        when(movieService.listMovies("english")).thenReturn(List.of(SKY_RIDERS));

        mockMvc.perform(get("/api/v1/movies").param("language", "english"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].language").value("English"));
    }

    @Test
    void getsOneMovie() throws Exception {
        when(movieService.getMovie(1L)).thenReturn(SKY_RIDERS);

        mockMvc.perform(get("/api/v1/movies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Sky Riders"))
                .andExpect(jsonPath("$.certificate").value("UA"))
                .andExpect(jsonPath("$.releaseDate").value("2026-10-02"));
    }

    @Test
    void returns404WhenMovieMissing() throws Exception {
        when(movieService.getMovie(99L)).thenThrow(new NotFoundException("Movie 99 not found"));

        mockMvc.perform(get("/api/v1/movies/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Movie 99 not found"));
    }

    @Test
    void returns400ForZeroId() throws Exception {
        mockMvc.perform(get("/api/v1/movies/0")).andExpect(status().isBadRequest());
    }
}
