package com.bookmyshow.modules.show;

import com.bookmyshow.common.error.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sold seats. Used by the booking module to sell, free, and transfer seats, and by the seat map to show BOOKED. */
@Service
@Transactional(readOnly = true)
public class ShowSeatBookingService {

    private final ShowRepository showRepository;
    private final ShowBookedSeatRepository bookedSeatRepository;
    private final Clock clock;

    public ShowSeatBookingService(
            ShowRepository showRepository, ShowBookedSeatRepository bookedSeatRepository, Clock clock) {
        this.showRepository = showRepository;
        this.bookedSeatRepository = bookedSeatRepository;
        this.clock = clock;
    }

    /**
     * Sells the seats to a booking. Returns false, and sells nothing, if any seat is already sold. The show row is
     * locked first, so two sales for the same show cannot pass the check at the same time.
     */
    @Transactional
    public boolean sellSeats(long showId, List<Long> seatIds, long bookingId) {
        lockShow(showId);
        if (bookedSeatRepository.existsByShowIdAndSeatIdInAndReleasedAtIsNull(showId, seatIds)) {
            return false;
        }
        bookedSeatRepository.saveAll(seatIds.stream()
                .map(seatId -> new ShowBookedSeat(showId, seatId, bookingId, clock.instant()))
                .toList());
        return true;
    }

    /** Undoes a sale (cancel). The seats can be sold again. Returns the freed seat IDs. */
    @Transactional
    public List<Long> releaseSeats(long showId, long bookingId) {
        lockShow(showId);
        List<ShowBookedSeat> sold = bookedSeatRepository.findByBookingIdAndReleasedAtIsNull(bookingId);
        sold.forEach(seat -> seat.release(clock.instant()));
        return sold.stream().map(ShowBookedSeat::getSeatId).toList();
    }

    /** Resale: moves the sold seats of one booking to another. Returns false if the first booking holds no seats. */
    @Transactional
    public boolean transferSeats(long showId, long fromBookingId, long toBookingId) {
        lockShow(showId);
        List<ShowBookedSeat> sold = bookedSeatRepository.findByBookingIdAndReleasedAtIsNull(fromBookingId);
        sold.forEach(seat -> seat.transferTo(toBookingId));
        return !sold.isEmpty();
    }

    public Set<Long> soldSeats(long showId) {
        return Set.copyOf(bookedSeatRepository.findSeatIdsByShowId(showId));
    }

    private void lockShow(long showId) {
        showRepository
                .findByIdForUpdate(showId)
                .orElseThrow(() -> new NotFoundException("Show " + showId + " not found"));
    }
}
