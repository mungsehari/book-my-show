package com.bookmyshow.modules.waitlist;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.TestLogins;
import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.modules.waitlist.WaitlistTypes.WaitlistResponse;
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
@WebMvcTest(WaitlistController.class)
class WaitlistControllerTest {

    private static final CurrentUser ASHA = new CurrentUser(7L, Roles.USER);
    private static final WaitlistResponse WAITING =
            new WaitlistResponse(3L, 50L, 2, WaitlistStatus.WAITING, 5L, null, List.of(), null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WaitlistService waitlistService;

    @Test
    void joins() throws Exception {
        when(waitlistService.join(50L, 2, ASHA)).thenReturn(WAITING);

        mockMvc.perform(post("/api/v1/shows/50/waitlist")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seats\": 2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(5));
    }

    @Test
    void joinNeedsLogin() throws Exception {
        mockMvc.perform(post("/api/v1/shows/50/waitlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seats\": 2}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMoreThanTenSeats() throws Exception {
        mockMvc.perform(post("/api/v1/shows/50/waitlist")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seats\": 11}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void myEntryNeedsLogin() throws Exception {
        mockMvc.perform(get("/api/v1/waitlist/me").param("showId", "50")).andExpect(status().isUnauthorized());
    }

    @Test
    void showsMyEntry() throws Exception {
        when(waitlistService.myEntry(50L, ASHA)).thenReturn(WAITING);

        mockMvc.perform(get("/api/v1/waitlist/me").param("showId", "50").with(TestLogins.as(7L, Roles.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"));
    }
}
