package com.bookmyshow.modules.show;

import com.bookmyshow.common.error.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sold seats. Used by the booking module to sell seats, and by the seat map to show them as BOOKED. */
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
        showRepository
                .findByIdForUpdate(showId)
                .orElseThrow(() -> new NotFoundException("Show " + showId + " not found"));
        if (bookedSeatRepository.existsByShowIdAndSeatIdIn(showId, seatIds)) {
            return false;
        }
        bookedSeatRepository.saveAll(seatIds.stream()
                .map(seatId -> new ShowBookedSeat(showId, seatId, bookingId, clock.instant()))
                .toList());
        return true;
    }

    public Set<Long> soldSeats(long showId) {
        return Set.copyOf(bookedSeatRepository.findSeatIdsByShowId(showId));
    }
}
