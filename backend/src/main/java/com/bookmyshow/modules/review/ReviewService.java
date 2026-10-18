package com.bookmyshow.modules.review;

import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.modules.booking.BookingService;
import com.bookmyshow.modules.movie.MovieService;
import com.bookmyshow.modules.review.ReviewRepository.RatingSummary;
import com.bookmyshow.modules.review.ReviewTypes.CreateReviewRequest;
import com.bookmyshow.modules.review.ReviewTypes.ReviewPageResponse;
import com.bookmyshow.modules.review.ReviewTypes.ReviewResponse;
import com.bookmyshow.modules.user.UserService;
import com.bookmyshow.security.CurrentUser;
import java.time.Clock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verified reviews. Only a user with a paid booking for the movie, whose show has started (or who was checked in), can
 * post. One review per user per movie. This keeps fake reviews out.
 */
@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MovieService movieService;
    private final BookingService bookingService;
    private final UserService userService;
    private final Clock clock;

    public ReviewService(
            ReviewRepository reviewRepository,
            MovieService movieService,
            BookingService bookingService,
            UserService userService,
            Clock clock) {
        this.reviewRepository = reviewRepository;
        this.movieService = movieService;
        this.bookingService = bookingService;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional
    public ReviewResponse create(long movieId, CreateReviewRequest request, CurrentUser user) {
        movieService.ensureExists(movieId);
        if (reviewRepository.existsByMovieIdAndUserId(movieId, user.id())) {
            throw new ConflictException("You already reviewed movie " + movieId);
        }
        long bookingId = bookingService
                .findWatchedBooking(user.id(), movieId)
                .orElseThrow(() -> new ForbiddenException("Only people who watched this movie with a ticket from us "
                        + "can review it. You can post once your show has started."));
        String text = request.text() == null || request.text().isBlank()
                ? null
                : request.text().strip();
        Review review = reviewRepository.save(
                new Review(movieId, user.id(), bookingId, request.rating(), text, clock.instant()));
        return toResponse(review);
    }

    /** Newest first, with the movie's average rating. */
    public ReviewPageResponse list(long movieId, int page, int size) {
        movieService.ensureExists(movieId);
        Page<Review> reviews = reviewRepository.findByMovieId(
                movieId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        RatingSummary summary = reviewRepository.summarize(movieId);
        Double average =
                summary == null || summary.getAverage() == null ? null : Math.round(summary.getAverage() * 10) / 10.0;
        long count = summary == null || summary.getCount() == null ? 0 : summary.getCount();
        return new ReviewPageResponse(
                movieId,
                average,
                count,
                reviews.getContent().stream().map(this::toResponse).toList(),
                reviews.getNumber(),
                reviews.getSize(),
                reviews.getTotalPages());
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getMovieId(),
                userService.getUser(review.getUserId()).name(),
                review.getRating(),
                review.getText(),
                true,
                review.getCreatedAt());
    }
}
