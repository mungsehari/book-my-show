package com.bookmyshow.modules.screen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
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
import com.bookmyshow.security.Roles;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ScreenServiceTest {

    private ScreenRepository screenRepository;
    private SeatRepository seatRepository;
    private CinemaService cinemaService;
    private CinemaAccessService cinemaAccessService;
    private ScreenService screenService;

    @BeforeEach
    void setUp() {
        screenRepository = mock(ScreenRepository.class);
        seatRepository = mock(SeatRepository.class);
        cinemaService = mock(CinemaService.class);
        cinemaAccessService = mock(CinemaAccessService.class);
        screenService = new ScreenService(screenRepository, seatRepository, cinemaService, cinemaAccessService);
    }

    @Test
    void listsScreensWithSeatCounts() {
        when(screenRepository.findAllByCinemaIdOrderByNameAsc(10L))
                .thenReturn(List.of(screen(1L, "Screen 1"), screen(2L, "Screen 2")));
        when(seatRepository.countByScreenIds(List.of(1L, 2L))).thenReturn(List.of(seatCount(1L, 116L)));

        List<ScreenResponse> result = screenService.listByCinema(10L);

        assertThat(result)
                .containsExactly(
                        new ScreenResponse(1L, 10L, "Screen 1", 116L), new ScreenResponse(2L, 10L, "Screen 2", 0L));
    }

    @Test
    void skipsSeatCountWhenCinemaHasNoScreens() {
        when(screenRepository.findAllByCinemaIdOrderByNameAsc(10L)).thenReturn(List.of());

        assertThat(screenService.listByCinema(10L)).isEmpty();
        verifyNoInteractions(seatRepository);
    }

    @Test
    void failsWhenCinemaMissing() {
        doThrow(new NotFoundException("Cinema 99 not found"))
                .when(cinemaService)
                .ensureExists(99L);

        assertThatThrownBy(() -> screenService.listByCinema(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Cinema 99 not found");
    }

    @Test
    void groupsSeatsByRowInOrder() {
        when(screenRepository.findById(1L)).thenReturn(Optional.of(screen(1L, "Screen 1")));
        when(seatRepository.findAllByScreenIdOrderByRowLabelAscSeatNumberAsc(1L))
                .thenReturn(List.of(
                        seat(1L, "A", 1, "REGULAR"), seat(2L, "A", 2, "REGULAR"), seat(3L, "J", 1, "RECLINER")));

        LayoutResponse layout = screenService.getLayout(1L);

        assertThat(layout.seatCount()).isEqualTo(3);
        assertThat(layout.rows()).hasSize(2);
        assertThat(layout.rows().get(0).label()).isEqualTo("A");
        assertThat(layout.rows().get(0).seats()).extracting("number").containsExactly(1, 2);
        assertThat(layout.rows().get(1).label()).isEqualTo("J");
        assertThat(layout.rows().get(1).seats().get(0).category()).isEqualTo("RECLINER");
    }

    @Test
    void failsWhenScreenMissing() {
        when(screenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> screenService.getLayout(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Screen 99 not found");
    }

    @Test
    void ensureExistsFailsWhenScreenMissing() {
        when(screenRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> screenService.ensureExists(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Screen 99 not found");
    }

    @Test
    void createsScreenWithSeatsPerRow() {
        CurrentUser user = new CurrentUser(2L, Roles.THEATRE_ADMIN);
        when(screenRepository.save(any(Screen.class))).thenAnswer(call -> {
            Screen saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 3L);
            return saved;
        });
        when(seatRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));

        LayoutResponse layout = screenService.create(
                10L,
                new CreateScreenRequest(
                        "Screen 3",
                        List.of(new RowRequest("B", 2, "PREMIUM", null), new RowRequest("A", 3, "REGULAR", null))),
                user);

        verify(cinemaAccessService).ensureCanManage(user, 10L);
        assertThat(layout.screenId()).isEqualTo(3L);
        assertThat(layout.seatCount()).isEqualTo(5);
        assertThat(layout.rows()).extracting(RowResponse::label).containsExactly("A", "B");
        assertThat(layout.rows().get(1).seats()).extracting("category").containsOnly("PREMIUM");
    }

    @Test
    void marksWheelchairSeats() {
        when(screenRepository.save(any(Screen.class))).thenAnswer(call -> {
            Screen saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 3L);
            return saved;
        });
        when(seatRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));

        LayoutResponse layout = screenService.create(
                10L,
                new CreateScreenRequest("Screen 3", List.of(new RowRequest("A", 3, "REGULAR", List.of(1, 3)))),
                new CurrentUser(1L, Roles.ADMIN));

        assertThat(layout.rows().get(0).seats())
                .extracting(SeatResponse::wheelchair)
                .containsExactly(true, false, true);
    }

    @Test
    void rejectsWheelchairSeatPastRowEnd() {
        CreateScreenRequest request =
                new CreateScreenRequest("Screen 3", List.of(new RowRequest("A", 3, "REGULAR", List.of(4))));

        assertThatThrownBy(() -> screenService.create(10L, request, new CurrentUser(1L, Roles.ADMIN)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Wheelchair seat A4 is past the end of the row");
    }

    @Test
    void rejectsRepeatedRow() {
        CreateScreenRequest request = new CreateScreenRequest(
                "Screen 3", List.of(new RowRequest("A", 2, "REGULAR", null), new RowRequest("A", 3, "REGULAR", null)));

        assertThatThrownBy(() -> screenService.create(10L, request, new CurrentUser(1L, Roles.ADMIN)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Row A is listed more than once");
        verify(screenRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateScreenName() {
        when(screenRepository.existsByCinemaIdAndNameIgnoreCase(10L, "Screen 1"))
                .thenReturn(true);
        CreateScreenRequest request =
                new CreateScreenRequest("Screen 1", List.of(new RowRequest("A", 2, "REGULAR", null)));

        assertThatThrownBy(() -> screenService.create(10L, request, new CurrentUser(1L, Roles.ADMIN)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void stopsWhenUserCannotManageCinema() {
        CurrentUser user = new CurrentUser(2L, Roles.THEATRE_ADMIN);
        doThrow(new ForbiddenException("You cannot manage cinema 10"))
                .when(cinemaAccessService)
                .ensureCanManage(user, 10L);
        CreateScreenRequest request =
                new CreateScreenRequest("Screen 3", List.of(new RowRequest("A", 2, "REGULAR", null)));

        assertThatThrownBy(() -> screenService.create(10L, request, user)).isInstanceOf(ForbiddenException.class);
        verify(screenRepository, never()).save(any());
    }

    private static Screen screen(long id, String name) {
        Screen screen = new Screen(10L, name);
        ReflectionTestUtils.setField(screen, "id", id);
        return screen;
    }

    private static Seat seat(long id, String row, int number, String category) {
        Seat seat = new Seat(1L, row, number, category);
        ReflectionTestUtils.setField(seat, "id", id);
        return seat;
    }

    private static SeatCount seatCount(long screenId, long count) {
        return new SeatCount() {
            @Override
            public Long getScreenId() {
                return screenId;
            }

            @Override
            public Long getSeatCount() {
                return count;
            }
        };
    }
}
