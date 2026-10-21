package com.bookmyshow.modules.booking;

import com.bookmyshow.common.response.PageResponse;
import com.bookmyshow.modules.booking.BookingTypes.BookingResponse;
import com.bookmyshow.modules.booking.BookingTypes.SaleFact;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only booking numbers for the admin dashboard. Every method takes the cinemas the admin may see, so a theatre
 * admin never gets data from other cinemas. An empty list of cinemas always gives an empty answer.
 */
@Service
@Transactional(readOnly = true)
public class BookingStatsService {

    private final BookingRepository bookingRepository;

    public BookingStatsService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /** Confirmed sales since a moment. */
    public List<SaleFact> sales(Collection<Long> cinemaIds, Instant from) {
        if (cinemaIds.isEmpty()) {
            return List.of();
        }
        return bookingRepository
                .findByStatusAndConfirmedAtGreaterThanEqualAndCinemaIdIn(BookingStatus.CONFIRMED, from, cinemaIds)
                .stream()
                .map(booking -> new SaleFact(
                        booking.getConfirmedAt(),
                        booking.getAmount(),
                        booking.getMovieId(),
                        booking.getMovieName(),
                        booking.getSeatIds().size()))
                .toList();
    }

    /** Bookings cancelled and refunded, created since a moment. */
    public long cancelledSince(Collection<Long> cinemaIds, Instant from) {
        if (cinemaIds.isEmpty()) {
            return 0;
        }
        return bookingRepository.countByStatusAndCreatedAtGreaterThanEqualAndCinemaIdIn(
                BookingStatus.CANCELLED, from, cinemaIds);
    }

    /** Paid bookings whose seats were sold to someone else. The money must go back. */
    public long refundsPending(Collection<Long> cinemaIds) {
        if (cinemaIds.isEmpty()) {
            return 0;
        }
        return bookingRepository.countByStatusAndCinemaIdIn(BookingStatus.NEEDS_REFUND, cinemaIds);
    }

    /** Newest first. status is optional. */
    public PageResponse<BookingResponse> list(Collection<Long> cinemaIds, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (cinemaIds.isEmpty()) {
            return PageResponse.of(Page.empty(pageable), BookingResponse::from);
        }
        Page<Booking> found = status == null
                ? bookingRepository.findByCinemaIdIn(cinemaIds, pageable)
                : bookingRepository.findByStatusAndCinemaIdIn(status, cinemaIds, pageable);
        return PageResponse.of(found, BookingResponse::from);
    }
}
