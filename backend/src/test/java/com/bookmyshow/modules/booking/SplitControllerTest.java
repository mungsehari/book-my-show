package com.bookmyshow.modules.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.TestLogins;
import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.security.Roles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithSecurityConfig
@WebMvcTest(SplitController.class)
class SplitControllerTest {

    private static final String LOCK_ID = "3f2b8c1e-7a4d-4e5f-9b6a-1c2d3e4f5a6b";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SplitService splitService;

    @Test
    void needsLogin() throws Exception {
        mockMvc.perform(get("/api/v1/payment-shares/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void atMostNineFriends() throws Exception {
        mockMvc.perform(post("/api/v1/bookings/split")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lockId\": \"" + LOCK_ID + "\", \"friendIds\": [1,2,3,4,5,6,8,9,10,11]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void needsAtLeastOneFriend() throws Exception {
        mockMvc.perform(post("/api/v1/bookings/split")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lockId\": \"" + LOCK_ID + "\", \"friendIds\": []}"))
                .andExpect(status().isBadRequest());
    }
}
