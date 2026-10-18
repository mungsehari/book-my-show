package com.bookmyshow.modules.show;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.TestLogins;
import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.modules.show.ShowTypes.CreateShowRequest;
import com.bookmyshow.modules.show.ShowTypes.ShowResponse;
import com.bookmyshow.security.Roles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithSecurityConfig
@WebMvcTest(ShowAdminController.class)
class ShowAdminControllerTest {

    private static final String BODY = """
            {"movieId": 2, "showDate": "2026-10-20", "startTime": "14:00",
             "prices": {"REGULAR": 200.00, "PREMIUM": 300.00}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShowService showService;

    @Test
    void createsShow() throws Exception {
        when(showService.create(eq(3L), any(CreateShowRequest.class), any()))
                .thenReturn(new ShowResponse(
                        50L,
                        10L,
                        3L,
                        2L,
                        "Sky Riders",
                        LocalDate.of(2026, 10, 20),
                        LocalTime.of(14, 0),
                        new BigDecimal("300.00"),
                        false,
                        false,
                        false));

        mockMvc.perform(post("/api/v1/admin/screens/3/shows")
                        .with(TestLogins.as(1L, Roles.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.screenId").value(3))
                .andExpect(jsonPath("$.startTime").value("14:00:00"));
    }

    @Test
    void overlapReturns409() throws Exception {
        when(showService.create(eq(3L), any(CreateShowRequest.class), any()))
                .thenThrow(new ConflictException("Screen 3 already has a show from 12:00 to 14:23 on 2026-10-20"));

        mockMvc.perform(post("/api/v1/admin/screens/3/shows")
                        .with(TestLogins.as(1L, Roles.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void rejectsMissingMovie() throws Exception {
        mockMvc.perform(post("/api/v1/admin/screens/3/shows")
                        .with(TestLogins.as(1L, Roles.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("\"movieId\": 2, ", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnknownPriceCategory() throws Exception {
        mockMvc.perform(post("/api/v1/admin/screens/3/shows")
                        .with(TestLogins.as(1L, Roles.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("PREMIUM", "GOLD")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void plainUserIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/screens/3/shows")
                        .with(TestLogins.as(5L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());
    }
}
