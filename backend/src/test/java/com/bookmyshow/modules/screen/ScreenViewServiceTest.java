package com.bookmyshow.modules.screen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.modules.cinema.CinemaAccessService;
import com.bookmyshow.modules.screen.ScreenTypes.CreateScreenViewRequest;
import com.bookmyshow.modules.screen.ScreenTypes.ScreenViewResponse;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScreenViewServiceTest {

    private static final CurrentUser STAFF = new CurrentUser(2L, Roles.THEATRE_ADMIN);
    private static final String URL = "https://img.example.com/screen1-front.jpg";

    private ScreenViewRepository viewRepository;
    private ScreenRepository screenRepository;
    private CinemaAccessService cinemaAccessService;
    private ScreenViewService service;

    @BeforeEach
    void setUp() {
        viewRepository = mock(ScreenViewRepository.class);
        screenRepository = mock(ScreenRepository.class);
        cinemaAccessService = mock(CinemaAccessService.class);
        service = new ScreenViewService(
                viewRepository,
                screenRepository,
                cinemaAccessService,
                Clock.fixed(Instant.parse("2026-10-18T05:00:00Z"), ZoneOffset.UTC));
        when(screenRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(new Screen(10L, "Screen 1")));
    }

    @Test
    void addsPhotoForRowBlock() {
        when(viewRepository.findByScreenIdOrderByFromRowAsc(3L))
                .thenReturn(List.of(new ScreenView(3L, "A", "C", URL, null, Instant.now())));
        when(viewRepository.save(any(ScreenView.class))).thenAnswer(call -> call.getArgument(0));

        ScreenViewResponse view = service.add(3L, new CreateScreenViewRequest("D", "F", URL, " Middle "), STAFF);

        verify(cinemaAccessService).ensureCanManage(STAFF, 10L);
        assertThat(view.fromRow()).isEqualTo("D");
        assertThat(view.caption()).isEqualTo("Middle");
    }

    @Test
    void rejectsOverlap() {
        when(viewRepository.findByScreenIdOrderByFromRowAsc(3L))
                .thenReturn(List.of(new ScreenView(3L, "A", "C", URL, null, Instant.now())));

        assertThatThrownBy(() -> service.add(3L, new CreateScreenViewRequest("C", "E", URL, null), STAFF))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("overlap");
        verify(viewRepository, never()).save(any());
    }

    @Test
    void rejectsBackwardsRows() {
        assertThatThrownBy(() -> service.add(3L, new CreateScreenViewRequest("F", "D", URL, null), STAFF))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void needsAccessToCinema() {
        doThrow(new ForbiddenException("You cannot manage cinema 10"))
                .when(cinemaAccessService)
                .ensureCanManage(STAFF, 10L);

        assertThatThrownBy(() -> service.add(3L, new CreateScreenViewRequest("A", "B", URL, null), STAFF))
                .isInstanceOf(ForbiddenException.class);
    }
}
