package com.bookmyshow.modules.screen;

import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.cinema.CinemaService;
import com.bookmyshow.modules.screen.ScreenTypes.LayoutResponse;
import com.bookmyshow.modules.screen.ScreenTypes.RowResponse;
import com.bookmyshow.modules.screen.ScreenTypes.ScreenResponse;
import com.bookmyshow.modules.screen.ScreenTypes.SeatResponse;
import com.bookmyshow.modules.screen.SeatRepository.SeatCount;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ScreenService {

    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final CinemaService cinemaService;

    public ScreenService(
            ScreenRepository screenRepository, SeatRepository seatRepository, CinemaService cinemaService) {
        this.screenRepository = screenRepository;
        this.seatRepository = seatRepository;
        this.cinemaService = cinemaService;
    }

    public List<ScreenResponse> listByCinema(long cinemaId) {
        cinemaService.ensureExists(cinemaId);
        List<Screen> screens = screenRepository.findAllByCinemaIdOrderByNameAsc(cinemaId);
        if (screens.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> counts = seatRepository
                .countByScreenIds(screens.stream().map(Screen::getId).toList())
                .stream()
                .collect(Collectors.toMap(SeatCount::getScreenId, SeatCount::getSeatCount));
        return screens.stream()
                .map(screen -> ScreenResponse.from(screen, counts.getOrDefault(screen.getId(), 0L)))
                .toList();
    }

    /** Returns the seat map of a screen, grouped by row. Rows and seats keep the database order. */
    public LayoutResponse getLayout(long screenId) {
        Screen screen = screenRepository.findById(screenId).orElseThrow(() -> notFound(screenId));
        List<Seat> seats = seatRepository.findAllByScreenIdOrderByRowLabelAscSeatNumberAsc(screenId);

        Map<String, List<SeatResponse>> byRow = new LinkedHashMap<>();
        for (Seat seat : seats) {
            byRow.computeIfAbsent(seat.getRowLabel(), label -> new ArrayList<>())
                    .add(SeatResponse.from(seat));
        }
        List<RowResponse> rows = byRow.entrySet().stream()
                .map(entry -> new RowResponse(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();

        return new LayoutResponse(screen.getId(), screen.getCinemaId(), screen.getName(), seats.size(), rows);
    }

    /** Used by other modules to check a screen before using its ID. */
    public void ensureExists(long screenId) {
        if (!screenRepository.existsById(screenId)) {
            throw notFound(screenId);
        }
    }

    private static NotFoundException notFound(long screenId) {
        return new NotFoundException("Screen " + screenId + " not found");
    }
}
