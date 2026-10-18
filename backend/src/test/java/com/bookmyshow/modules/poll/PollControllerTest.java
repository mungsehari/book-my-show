package com.bookmyshow.modules.poll;

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
@WebMvcTest(PollController.class)
class PollControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PollService pollService;

    @Test
    void needsLogin() throws Exception {
        mockMvc.perform(get("/api/v1/polls/mine")).andExpect(status().isUnauthorized());
    }

    @Test
    void needsTwoToFiveShows() throws Exception {
        mockMvc.perform(post("/api/v1/polls")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Friday\", \"showIds\": [50], \"friendIds\": []}"))
                .andExpect(status().isBadRequest());
    }
}
