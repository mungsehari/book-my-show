package com.bookmyshow.modules.alert;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.TestLogins;
import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.modules.alert.PriceAlertTypes.CreatePriceAlertRequest;
import com.bookmyshow.modules.alert.PriceAlertTypes.PriceAlertResponse;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithSecurityConfig
@WebMvcTest(PriceAlertController.class)
class PriceAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PriceAlertService alertService;

    @Test
    void createsAlert() throws Exception {
        when(alertService.create(
                        new CreatePriceAlertRequest(2L, 1L, new BigDecimal("150")), new CurrentUser(7L, Roles.USER)))
                .thenReturn(new PriceAlertResponse(
                        1L,
                        2L,
                        1L,
                        new BigDecimal("150"),
                        PriceAlertStatus.ACTIVE,
                        Instant.parse("2026-10-18T05:00:00Z"),
                        null,
                        null,
                        null));

        mockMvc.perform(post("/api/v1/price-alerts")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\": 2, \"cityId\": 1, \"maxPrice\": 150}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void rejectsZeroPrice() throws Exception {
        mockMvc.perform(post("/api/v1/price-alerts")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\": 2, \"cityId\": 1, \"maxPrice\": 0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void needsLogin() throws Exception {
        mockMvc.perform(get("/api/v1/price-alerts/me")).andExpect(status().isUnauthorized());
    }
}
