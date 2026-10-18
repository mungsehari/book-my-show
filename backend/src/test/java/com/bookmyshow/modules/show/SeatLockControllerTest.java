package com.bookmyshow.modules.show;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.TestLogins;
import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.modules.show.ShowTypes.SeatLockResponse;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithSecurityConfig
@WebMvcTest(SeatLockController.class)
class SeatLockControllerTest {

    private static final String LOCK_ID = "3f2b8c1e-7a4d-4e5f-9b6a-1c2d3e4f5a6b";
    private static final CurrentUser ASHA = new CurrentUser(7L, Roles.USER);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShowSeatLockService seatLockService;

    @Test
    void locksSeats() throws Exception {
        when(seatLockService.lock(50L, List.of(1L, 2L), ASHA))
                .thenReturn(new SeatLockResponse(
                        LOCK_ID,
                        50L,
                        List.of(1L, 2L),
                        new BigDecimal("400.00"),
                        Instant.parse("2026-10-18T04:40:00Z")));

        mockMvc.perform(post("/api/v1/shows/50/locks")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seatIds": [1, 2]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lockId").value(LOCK_ID))
                .andExpect(jsonPath("$.totalPrice").value(400.00));
    }

    @Test
    void lockNeedsLogin() throws Exception {
        mockMvc.perform(post("/api/v1/shows/50/locks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seatIds": [1]}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMoreThanTenSeats() throws Exception {
        mockMvc.perform(post("/api/v1/shows/50/locks")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seatIds": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void releasesLock() throws Exception {
        mockMvc.perform(post("/api/v1/locks/" + LOCK_ID + "/release").with(TestLogins.as(7L, Roles.USER)))
                .andExpect(status().isNoContent());

        verify(seatLockService).release(eq(LOCK_ID), eq(ASHA));
    }

    @Test
    void rejectsMalformedLockId() throws Exception {
        mockMvc.perform(post("/api/v1/locks/not-a-uuid/release").with(TestLogins.as(7L, Roles.USER)))
                .andExpect(status().isBadRequest());
    }
}
