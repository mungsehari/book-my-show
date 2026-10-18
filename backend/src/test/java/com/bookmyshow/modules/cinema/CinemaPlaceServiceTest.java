package com.bookmyshow.modules.cinema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.modules.cinema.CinemaTypes.CreatePlaceRequest;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CinemaPlaceServiceTest {

    private static final CurrentUser STAFF = new CurrentUser(2L, Roles.THEATRE_ADMIN);

    private CinemaPlaceRepository placeRepository;
    private CinemaAccessService accessService;
    private CinemaPlaceService service;

    @BeforeEach
    void setUp() {
        placeRepository = mock(CinemaPlaceRepository.class);
        accessService = mock(CinemaAccessService.class);
        service = new CinemaPlaceService(
                placeRepository,
                mock(CinemaService.class),
                accessService,
                Clock.fixed(Instant.parse("2026-10-18T05:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void staffAddsPlace() {
        when(placeRepository.save(any(CinemaPlace.class))).thenAnswer(call -> call.getArgument(0));

        var place = service.add(10L, new CreatePlaceRequest("FOOD", " Chai Point ", 3, " "), STAFF);

        verify(accessService).ensureCanManage(STAFF, 10L);
        assertThat(place.name()).isEqualTo("Chai Point");
        assertThat(place.notes()).isNull();
    }

    @Test
    void otherStaffCannotAdd() {
        doThrow(new ForbiddenException("You cannot manage cinema 10"))
                .when(accessService)
                .ensureCanManage(STAFF, 10L);

        assertThatThrownBy(() -> service.add(10L, new CreatePlaceRequest("FOOD", "Chai", 3, null), STAFF))
                .isInstanceOf(ForbiddenException.class);
        verify(placeRepository, never()).save(any());
    }
}
