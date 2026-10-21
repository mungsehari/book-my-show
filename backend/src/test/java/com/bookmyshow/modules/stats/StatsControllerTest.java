package com.bookmyshow.modules.stats;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.TestLogins;
import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.modules.stats.StatsTypes.SummaryResponse;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.math.BigDecimal;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithSecurityConfig
@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsService statsService;

    @MockitoBean
    private Clock clock;

    @Test
    void needsLogin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats/summary")).andExpect(status().isUnauthorized());
    }

    @Test
    void plainUsersAreForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats/summary").with(TestLogins.as(5L, Roles.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/bookings").with(TestLogins.as(5L, Roles.USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void theatreAdminGetsSummaryWithSevenDaysByDefault() throws Exception {
        when(statsService.summary(any(CurrentUser.class), eq(7)))
                .thenReturn(new SummaryResponse(7, 2, 4, new BigDecimal("400.00"), new BigDecimal("100.00"), 1, 0));

        mockMvc.perform(get("/api/v1/admin/stats/summary").with(TestLogins.as(3L, Roles.THEATRE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketsSold").value(4))
                .andExpect(jsonPath("$.revenue").value(400.00));
    }

    @Test
    void daysOutsideTheLimitsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats/daily?days=0").with(TestLogins.as(1L, Roles.ADMIN)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/admin/stats/daily?days=91").with(TestLogins.as(1L, Roles.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statusFilterMustLookLikeAStatus() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings?status=drop table").with(TestLogins.as(1L, Roles.ADMIN)))
                .andExpect(status().isBadRequest());
    }
}
