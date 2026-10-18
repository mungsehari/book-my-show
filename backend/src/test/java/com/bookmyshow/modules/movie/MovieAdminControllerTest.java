package com.bookmyshow.modules.movie;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.TestLogins;
import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.modules.movie.MovieTypes.CreateMovieRequest;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
import com.bookmyshow.security.Roles;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithSecurityConfig
@WebMvcTest(MovieAdminController.class)
class MovieAdminControllerTest {

    private static final String BODY = """
            {"title": "Sky Riders", "description": "Plot.", "language": "English", "genre": "Action",
             "durationMinutes": 128, "certificate": "UA", "releaseDate": "2026-10-02"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @Test
    void adminCreatesMovie() throws Exception {
        when(movieService.create(any(CreateMovieRequest.class)))
                .thenReturn(new MovieResponse(
                        4L, "Sky Riders", "Plot.", "English", "Action", 128, "UA", LocalDate.of(2026, 10, 2), null));

        mockMvc.perform(post("/api/v1/admin/movies")
                        .with(TestLogins.as(1L, Roles.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4));
    }

    @Test
    void theatreAdminCannotCreateMovie() throws Exception {
        mockMvc.perform(post("/api/v1/admin/movies")
                        .with(TestLogins.as(2L, Roles.THEATRE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsUnknownCertificate() throws Exception {
        mockMvc.perform(post("/api/v1/admin/movies")
                        .with(TestLogins.as(1L, Roles.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("\"UA\"", "\"PG\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for 'certificate': must match \"^(U|UA|A)$\""));
    }
}
