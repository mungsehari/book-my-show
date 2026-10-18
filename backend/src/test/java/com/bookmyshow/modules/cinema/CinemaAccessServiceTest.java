package com.bookmyshow.modules.cinema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.modules.user.UserService;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CinemaAccessServiceTest {

    private CinemaAdminRepository cinemaAdminRepository;
    private CinemaService cinemaService;
    private UserService userService;
    private CinemaAccessService accessService;

    @BeforeEach
    void setUp() {
        cinemaAdminRepository = mock(CinemaAdminRepository.class);
        cinemaService = mock(CinemaService.class);
        userService = mock(UserService.class);
        accessService = new CinemaAccessService(
                cinemaAdminRepository,
                cinemaService,
                userService,
                Clock.fixed(Instant.parse("2026-10-18T06:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void adminManagesAnyCinema() {
        assertThatCode(() -> accessService.ensureCanManage(new CurrentUser(1L, Roles.ADMIN), 10L))
                .doesNotThrowAnyException();
        verify(cinemaService).ensureExists(10L);
    }

    @Test
    void theatreAdminManagesAssignedCinema() {
        when(cinemaAdminRepository.existsByUserIdAndCinemaId(2L, 10L)).thenReturn(true);

        assertThatCode(() -> accessService.ensureCanManage(new CurrentUser(2L, Roles.THEATRE_ADMIN), 10L))
                .doesNotThrowAnyException();
    }

    @Test
    void theatreAdminCannotManageOtherCinema() {
        when(cinemaAdminRepository.existsByUserIdAndCinemaId(2L, 11L)).thenReturn(false);

        assertThatThrownBy(() -> accessService.ensureCanManage(new CurrentUser(2L, Roles.THEATRE_ADMIN), 11L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You cannot manage cinema 11");
    }

    @Test
    void plainUserCannotManageEvenWithStaleAssignment() {
        when(cinemaAdminRepository.existsByUserIdAndCinemaId(3L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> accessService.ensureCanManage(new CurrentUser(3L, Roles.USER), 10L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void assignsAdminAndGrantsRole() {
        when(cinemaAdminRepository.save(any(CinemaAdmin.class))).thenAnswer(call -> call.getArgument(0));

        var response = accessService.assignAdmin(10L, 2L);

        assertThat(response.cinemaId()).isEqualTo(10L);
        assertThat(response.userId()).isEqualTo(2L);
        verify(userService).ensureExists(2L);
        verify(userService).grantTheatreAdmin(2L);
    }

    @Test
    void rejectsDuplicateAssignment() {
        when(cinemaAdminRepository.existsByUserIdAndCinemaId(2L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> accessService.assignAdmin(10L, 2L)).isInstanceOf(ConflictException.class);
        verify(userService, never()).grantTheatreAdmin(2L);
    }
}
