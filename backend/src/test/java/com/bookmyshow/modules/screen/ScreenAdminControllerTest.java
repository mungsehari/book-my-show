package com.bookmyshow.modules.screen;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.TestLogins;
import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.modules.screen.ScreenTypes.CreateScreenRequest;
import com.bookmyshow.modules.screen.ScreenTypes.LayoutResponse;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithSecurityConfig
@WebMvcTest(ScreenAdminController.class)
class ScreenAdminControllerTest {

    private static final String BODY = """
            {"name": "Screen 3", "rows": [{"label": "A", "seatCount": 10, "category": "REGULAR"}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScreenService screenService;

    @MockitoBean
    private ScreenViewService screenViewService;

    @Test
    void seatViewNeedsHttpsUrl() throws Exception {
        mockMvc.perform(
                        post("/api/v1/admin/screens/3/views")
                                .with(TestLogins.as(1L, Roles.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"fromRow\": \"A\", \"toRow\": \"C\", \"imageUrl\": \"http://img.example.com/a.jpg\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void plainUserCannotAddSeatView() throws Exception {
        mockMvc.perform(
                        post("/api/v1/admin/screens/3/views")
                                .with(TestLogins.as(5L, Roles.USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"fromRow\": \"A\", \"toRow\": \"C\", \"imageUrl\": \"https://img.example.com/a.jpg\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void theatreAdminCreatesScreen() throws Exception {
        when(screenService.create(
                        eq(10L), any(CreateScreenRequest.class), eq(new CurrentUser(2L, Roles.THEATRE_ADMIN))))
                .thenReturn(new LayoutResponse(3L, 10L, "Screen 3", 10, List.of()));

        mockMvc.perform(post("/api/v1/admin/cinemas/10/screens")
                        .with(TestLogins.as(2L, Roles.THEATRE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.screenId").value(3));
    }

    @Test
    void plainUserIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/cinemas/10/screens")
                        .with(TestLogins.as(5L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsBadRowLabel() throws Exception {
        mockMvc.perform(post("/api/v1/admin/cinemas/10/screens")
                        .with(TestLogins.as(1L, Roles.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("\"A\"", "\"AA\"")))
                .andExpect(status().isBadRequest());
    }
}
