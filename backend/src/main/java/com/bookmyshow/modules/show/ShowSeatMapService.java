package com.bookmyshow.modules.show;

import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.screen.ScreenService;
import com.bookmyshow.modules.screen.ScreenTypes.LayoutResponse;
import com.bookmyshow.modules.screen.ScreenTypes.SeatResponse;
import com.bookmyshow.modules.show.ShowTypes.CategoryPrice;
import com.bookmyshow.modules.show.ShowTypes.SeatMapResponse;
import com.bookmyshow.modules.show.ShowTypes.SeatMapRow;
import com.bookmyshow.modules.show.ShowTypes.SeatMapSeat;
import com.bookmyshow.modules.show.ShowTypes.SeatStatus;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Seat map of one show: the screen layout with this show's prices and each seat's status. */
@Service
@Transactional(readOnly = true)
public class ShowSeatMapService {

    private final ShowRepository showRepository;
    private final ShowPriceRepository showPriceRepository;
    private final ScreenService screenService;
    private final SeatLockStore seatLockStore;

    public ShowSeatMapService(
            ShowRepository showRepository,
            ShowPriceRepository showPriceRepository,
            ScreenService screenService,
            SeatLockStore seatLockStore) {
        this.showRepository = showRepository;
        this.showPriceRepository = showPriceRepository;
        this.screenService = screenService;
        this.seatLockStore = seatLockStore;
    }

    /**
     * A seat is UNAVAILABLE if its category has no price, LOCKED if someone holds it in checkout, else AVAILABLE.
     * BOOKED comes with bookings.
     */
    public SeatMapResponse getSeatMap(long showId) {
        Show show = showRepository
                .findById(showId)
                .orElseThrow(() -> new NotFoundException("Show " + showId + " not found"));
        if (show.getScreenId() == null) {
            throw new NotFoundException("Show " + showId + " has no seat map, because it is not on a screen");
        }
        LayoutResponse layout = screenService.getLayout(show.getScreenId());
        Map<String, BigDecimal> prices = showPriceRepository.findAllByShowId(showId).stream()
                .collect(Collectors.toMap(ShowPrice::getCategory, ShowPrice::getPrice));

        List<Long> seatIds = layout.rows().stream()
                .flatMap(row -> row.seats().stream())
                .map(SeatResponse::id)
                .toList();
        Set<Long> locked = seatLockStore.lockedSeats(showId, seatIds);

        List<SeatMapRow> rows = layout.rows().stream()
                .map(row -> new SeatMapRow(
                        row.label(),
                        row.seats().stream()
                                .map(seat -> toSeat(seat, prices, locked))
                                .toList()))
                .toList();
        List<CategoryPrice> categoryPrices = prices.entrySet().stream()
                .map(entry -> new CategoryPrice(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CategoryPrice::price))
                .toList();

        return new SeatMapResponse(
                show.getId(),
                show.getMovieId(),
                show.getMovieName(),
                show.getShowDate(),
                show.getStartTime(),
                layout.screenId(),
                layout.name(),
                categoryPrices,
                rows);
    }

    private static SeatMapSeat toSeat(SeatResponse seat, Map<String, BigDecimal> prices, Set<Long> locked) {
        BigDecimal price = prices.get(seat.category());
        String status;
        if (price == null) {
            status = SeatStatus.UNAVAILABLE;
        } else if (locked.contains(seat.id())) {
            status = SeatStatus.LOCKED;
        } else {
            status = SeatStatus.AVAILABLE;
        }
        return new SeatMapSeat(seat.id(), seat.number(), seat.category(), price, status);
    }
}
