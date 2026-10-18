package com.bookmyshow.modules.cinema;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.TestLogins;
import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.modules.cinema.CinemaTypes.CinemaAdminResponse;
import com.bookmyshow.security.Roles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithSecurityConfig
@WebMvcTest(CinemaAdminController.class)
class CinemaAdminControllerTest {

    private static final String BODY = """
            {"userId": 2}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CinemaAccessService cinemaAccessService;

    @Test
    void needsToken() throws Exception {
        mockMvc.perform(post("/api/v1/admin/cinemas/10/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theatreAdminCannotAssign() throws Exception {
        mockMvc.perform(post("/api/v1/admin/cinemas/10/admins")
                        .with(TestLogins.as(2L, Roles.THEATRE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminAssigns() throws Exception {
        when(cinemaAccessService.assignAdmin(10L, 2L)).thenReturn(new CinemaAdminResponse(10L, 2L));

        mockMvc.perform(post("/api/v1/admin/cinemas/10/admins")
                        .with(TestLogins.as(1L, Roles.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(2));
    }

    @Test
    void rejectsMissingUserId() throws Exception {
        mockMvc.perform(post("/api/v1/admin/cinemas/10/admins")
                        .with(TestLogins.as(1L, Roles.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
