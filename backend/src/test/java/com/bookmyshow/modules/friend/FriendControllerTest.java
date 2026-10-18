package com.bookmyshow.modules.friend;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.TestLogins;
import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.modules.friend.FriendTypes.FriendSummary;
import com.bookmyshow.modules.friend.FriendTypes.FriendsActivityResponse;
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
@WebMvcTest(FriendController.class)
class FriendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FriendService friendService;

    @MockitoBean
    private FriendsActivityService activityService;

    @Test
    void everythingNeedsLogin() throws Exception {
        mockMvc.perform(get("/api/v1/friends")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/friends/activity").param("movieId", "2")).andExpect(status().isUnauthorized());
    }

    @Test
    void showsActivity() throws Exception {
        when(activityService.forMovie(2L, new CurrentUser(7L, Roles.USER)))
                .thenReturn(new FriendsActivityResponse(2L, 1, List.of(new FriendSummary(8L, "Ravi")), 0, List.of()));

        mockMvc.perform(get("/api/v1/friends/activity").param("movieId", "2").with(TestLogins.as(7L, Roles.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.watchedCount").value(1))
                .andExpect(jsonPath("$.watched[0].name").value("Ravi"));
    }

    @Test
    void requestNeedsEmail() throws Exception {
        mockMvc.perform(post("/api/v1/friends/requests")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }
}
