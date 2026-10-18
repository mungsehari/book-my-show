package com.bookmyshow.modules.review;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.TestLogins;
import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.modules.review.ReviewTypes.CreateReviewRequest;
import com.bookmyshow.modules.review.ReviewTypes.ReviewPageResponse;
import com.bookmyshow.modules.review.ReviewTypes.ReviewResponse;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithSecurityConfig
@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    private static final ReviewResponse REVIEW =
            new ReviewResponse(1L, 2L, "Asha", 4, "Good", true, Instant.parse("2026-10-18T05:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    void reviewsArePublic() throws Exception {
        when(reviewService.list(2L, 0, 20)).thenReturn(new ReviewPageResponse(2L, 4.0, 1, List.of(REVIEW), 0, 20, 1));

        mockMvc.perform(get("/api/v1/movies/2/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.0))
                .andExpect(jsonPath("$.items[0].verified").value(true));
    }

    @Test
    void postingNeedsLogin() throws Exception {
        mockMvc.perform(post("/api/v1/movies/2/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 4}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postsReview() throws Exception {
        when(reviewService.create(2L, new CreateReviewRequest(4, "Good"), new CurrentUser(7L, Roles.USER)))
                .thenReturn(REVIEW);

        mockMvc.perform(post("/api/v1/movies/2/reviews")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 4, \"text\": \"Good\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    void rejectsRatingAboveFive() throws Exception {
        mockMvc.perform(post("/api/v1/movies/2/reviews")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 6}"))
                .andExpect(status().isBadRequest());
    }
}
