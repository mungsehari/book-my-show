package com.bookmyshow.modules.screen;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.cinema.CinemaAccessService;
import com.bookmyshow.modules.cinema.CinemaService;
import com.bookmyshow.modules.screen.ScreenTypes.CreateScreenRequest;
import com.bookmyshow.modules.screen.ScreenTypes.LayoutResponse;
import com.bookmyshow.modules.screen.ScreenTypes.RowRequest;
import com.bookmyshow.modules.screen.ScreenTypes.RowResponse;
import com.bookmyshow.modules.screen.ScreenTypes.ScreenResponse;
import com.bookmyshow.modules.screen.ScreenTypes.SeatResponse;
import com.bookmyshow.modules.screen.SeatRepository.SeatCount;
import com.bookmyshow.security.CurrentUser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ScreenService {

    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final CinemaService cinemaService;
    private final CinemaAccessService cinemaAccessService;

    public ScreenService(
            ScreenRepository screenRepository,
            SeatRepository seatRepository,
            CinemaService cinemaService,
            CinemaAccessService cinemaAccessService) {
        this.screenRepository = screenRepository;
        this.seatRepository = seatRepository;
        this.cinemaService = cinemaService;
        this.cinemaAccessService = cinemaAccessService;
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
        return toLayout(screen, seats);
    }

    /** Creates a screen and all its seats in one transaction. */
    @Transactional
    public LayoutResponse create(long cinemaId, CreateScreenRequest request, CurrentUser user) {
        cinemaAccessService.ensureCanManage(user, cinemaId);
        String name = request.name().strip();
        if (screenRepository.existsByCinemaIdAndNameIgnoreCase(cinemaId, name)) {
            throw new ConflictException("Cinema " + cinemaId + " already has a screen named '" + name + "'");
        }
        Set<String> labels = new HashSet<>();
        for (RowRequest row : request.rows()) {
            if (!labels.add(row.label())) {
                throw new BadRequestException("Row " + row.label() + " is listed more than once");
            }
            for (Integer number : wheelchairSeats(row)) {
                if (number > row.seatCount()) {
                    throw new BadRequestException(
                            "Wheelchair seat " + row.label() + number + " is past the end of the row");
                }
            }
        }

        Screen screen = screenRepository.save(new Screen(cinemaId, name));
        List<Seat> seats = new ArrayList<>();
        request.rows().stream().sorted((a, b) -> a.label().compareTo(b.label())).forEach(row -> {
            Set<Integer> wheelchair = Set.copyOf(wheelchairSeats(row));
            for (int number = 1; number <= row.seatCount(); number++) {
                Seat seat = new Seat(screen.getId(), row.label(), number, row.category());
                if (wheelchair.contains(number)) {
                    seat.markWheelchair();
                }
                seats.add(seat);
            }
        });
        return toLayout(screen, seatRepository.saveAll(seats));
    }

    /**
     * Locks the screen until the caller's transaction ends and returns its cinema ID. Used by the show module so two
     * admins cannot add overlapping shows to the same screen at the same time.
     */
    @Transactional
    public long lockAndGetCinemaId(long screenId) {
        return screenRepository
                .findByIdForUpdate(screenId)
                .orElseThrow(() -> notFound(screenId))
                .getCinemaId();
    }

    /** Seat categories present on a screen, for example REGULAR and PREMIUM. Used to check show prices. */
    public Set<String> listCategories(long screenId) {
        return Set.copyOf(seatRepository.findCategoriesByScreenId(screenId));
    }

    /** Screens from the list that have at least one wheelchair seat. Used by the show filters. */
    public Set<Long> withWheelchairSeats(Collection<Long> screenIds) {
        return screenIds.isEmpty() ? Set.of() : Set.copyOf(seatRepository.findScreenIdsWithWheelchairSeats(screenIds));
    }

    /** Used by other modules to check a screen before using its ID. */
    public void ensureExists(long screenId) {
        if (!screenRepository.existsById(screenId)) {
            throw notFound(screenId);
        }
    }

    private static List<Integer> wheelchairSeats(RowRequest row) {
        return row.wheelchairSeats() == null ? List.of() : row.wheelchairSeats();
    }

    private static LayoutResponse toLayout(Screen screen, List<Seat> seats) {
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

    private static NotFoundException notFound(long screenId) {
        return new NotFoundException("Screen " + screenId + " not found");
    }
}
