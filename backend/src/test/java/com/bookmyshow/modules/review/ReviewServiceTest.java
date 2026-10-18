package com.bookmyshow.modules.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.modules.booking.BookingService;
import com.bookmyshow.modules.movie.MovieService;
import com.bookmyshow.modules.review.ReviewRepository.RatingSummary;
import com.bookmyshow.modules.review.ReviewTypes.CreateReviewRequest;
import com.bookmyshow.modules.review.ReviewTypes.ReviewPageResponse;
import com.bookmyshow.modules.review.ReviewTypes.ReviewResponse;
import com.bookmyshow.modules.user.UserService;
import com.bookmyshow.modules.user.UserTypes.UserResponse;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class ReviewServiceTest {

    private static final CurrentUser ASHA = new CurrentUser(7L, Roles.USER);

    private ReviewRepository reviewRepository;
    private BookingService bookingService;
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        bookingService = mock(BookingService.class);
        UserService userService = mock(UserService.class);
        when(userService.getUser(7L)).thenReturn(new UserResponse(7L, "Asha", null, null, Roles.USER));
        reviewService = new ReviewService(
                reviewRepository,
                mock(MovieService.class),
                bookingService,
                userService,
                Clock.fixed(Instant.parse("2026-10-18T05:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void watcherCanReview() {
        when(bookingService.findWatchedBooking(7L, 2L)).thenReturn(Optional.of(9L));
        when(reviewRepository.save(any(Review.class))).thenAnswer(call -> withId(call.getArgument(0)));

        ReviewResponse review =
                reviewService.create(2L, new CreateReviewRequest(4, "  Loved the flying scenes. "), ASHA);

        assertThat(review.rating()).isEqualTo(4);
        assertThat(review.text()).isEqualTo("Loved the flying scenes.");
        assertThat(review.userName()).isEqualTo("Asha");
        assertThat(review.verified()).isTrue();
    }

    @Test
    void nonWatcherCannotReview() {
        when(bookingService.findWatchedBooking(7L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.create(2L, new CreateReviewRequest(5, null), ASHA))
                .isInstanceOf(ForbiddenException.class);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void oneReviewPerMovie() {
        when(reviewRepository.existsByMovieIdAndUserId(2L, 7L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.create(2L, new CreateReviewRequest(5, null), ASHA))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void listsWithRoundedAverage() {
        Review review = withId(new Review(2L, 7L, 9L, 4, null, Instant.parse("2026-10-18T05:00:00Z")));
        when(reviewRepository.findByMovieId(eq(2L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 20), 1));
        when(reviewRepository.summarize(2L)).thenReturn(summary(4.333, 3L));

        ReviewPageResponse page = reviewService.list(2L, 0, 20);

        assertThat(page.averageRating()).isEqualTo(4.3);
        assertThat(page.reviewCount()).isEqualTo(3);
        assertThat(page.items())
                .singleElement()
                .satisfies(r -> assertThat(r.userName()).isEqualTo("Asha"));
    }

    @Test
    void noReviewsMeansNoAverage() {
        when(reviewRepository.findByMovieId(eq(2L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(reviewRepository.summarize(2L)).thenReturn(summary(null, 0L));

        ReviewPageResponse page = reviewService.list(2L, 0, 20);

        assertThat(page.averageRating()).isNull();
        assertThat(page.reviewCount()).isZero();
    }

    private static Review withId(Review review) {
        ReflectionTestUtils.setField(review, "id", 1L);
        return review;
    }

    private static RatingSummary summary(Double average, Long count) {
        return new RatingSummary() {
            @Override
            public Double getAverage() {
                return average;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }
}
