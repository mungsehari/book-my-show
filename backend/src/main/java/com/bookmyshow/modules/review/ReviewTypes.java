package com.bookmyshow.modules.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** All request and response types for the review module. */
public final class ReviewTypes {

    private ReviewTypes() {}

    public record CreateReviewRequest(
            @NotNull @Min(1) @Max(5) Integer rating,
            @Size(max = 1000) String text) {}

    /** verified is always true: only people who watched the movie can post. */
    public record ReviewResponse(
            Long id, Long movieId, String userName, Integer rating, String text, boolean verified, Instant createdAt) {}

    /** averageRating is rounded to one decimal, and null when there are no reviews. */
    public record ReviewPageResponse(
            Long movieId,
            Double averageRating,
            long reviewCount,
            List<ReviewResponse> items,
            int page,
            int size,
            int totalPages) {}
}
