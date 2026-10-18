package com.bookmyshow.modules.review;

import com.bookmyshow.modules.review.ReviewTypes.CreateReviewRequest;
import com.bookmyshow.modules.review.ReviewTypes.ReviewPageResponse;
import com.bookmyshow.modules.review.ReviewTypes.ReviewResponse;
import com.bookmyshow.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/api/v1/movies/{movieId}/reviews")
    @Operation(summary = "Verified reviews of a movie, newest first, with the average rating")
    public ReviewPageResponse list(
            @PathVariable @Positive long movieId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return reviewService.list(movieId, page, size);
    }

    @PostMapping("/api/v1/movies/{movieId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Review a movie you watched with a ticket from us. One review per movie.")
    public ReviewResponse create(
            @PathVariable @Positive long movieId,
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return reviewService.create(movieId, request, CurrentUser.from(jwt));
    }
}
