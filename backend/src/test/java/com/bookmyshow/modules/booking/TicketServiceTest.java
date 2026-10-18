package com.bookmyshow.modules.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.modules.booking.BookingTypes.TicketResponse;
import com.bookmyshow.modules.cinema.CinemaAccessService;
import com.bookmyshow.modules.cinema.CinemaService;
import com.bookmyshow.modules.cinema.CinemaTypes.CinemaResponse;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketServiceTest {

    // 2026-10-18 17:00 in India. The test booking is for 2026-10-18 18:00.
    private static final Clock SHOW_DAY = Clock.fixed(Instant.parse("2026-10-18T11:30:00Z"), ZoneId.of("Asia/Kolkata"));
    private static final CurrentUser ASHA = new CurrentUser(7L, Roles.USER);
    private static final CurrentUser STAFF = new CurrentUser(2L, Roles.THEATRE_ADMIN);

    private BookingRepository bookingRepository;
    private TicketCodes ticketCodes;
    private CinemaService cinemaService;
    private CinemaAccessService cinemaAccessService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        ticketCodes = mock(TicketCodes.class);
        cinemaService = mock(CinemaService.class);
        cinemaAccessService = mock(CinemaAccessService.class);
        when(ticketCodes.issue(9L)).thenReturn("bms1.9.sig");
        when(ticketCodes.verify("bms1.9.sig")).thenReturn(Optional.of(9L));
        when(cinemaService.getCinema(10L)).thenReturn(new CinemaResponse(10L, 1L, "City Cineplex Andheri", "Mumbai"));
    }

    @Test
    void ownerGetsTicketForConfirmedBooking() {
        when(bookingRepository.findById(9L)).thenReturn(Optional.of(confirmed()));

        TicketResponse ticket = service(SHOW_DAY).getTicket(9L, ASHA);

        assertThat(ticket.ticketCode()).isEqualTo("bms1.9.sig");
        assertThat(ticket.cinemaName()).isEqualTo("City Cineplex Andheri");
        assertThat(ticket.seats()).containsExactly("A1");
        assertThat(ticket.checkedInAt()).isNull();
    }

    @Test
    void unpaidBookingHasNoTicket() {
        when(bookingRepository.findById(9L)).thenReturn(Optional.of(BookingServiceTest.pending()));

        assertThatThrownBy(() -> service(SHOW_DAY).getTicket(9L, ASHA)).isInstanceOf(ConflictException.class);
    }

    @Test
    void otherUserCannotSeeTicket() {
        when(bookingRepository.findById(9L)).thenReturn(Optional.of(confirmed()));

        assertThatThrownBy(() -> service(SHOW_DAY).getTicket(9L, new CurrentUser(8L, Roles.USER)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void checkInWorksOnceOnShowDay() {
        Booking booking = confirmed();
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(booking));
        TicketService service = service(SHOW_DAY);

        TicketResponse first = service.checkIn("bms1.9.sig", STAFF);

        assertThat(first.checkedInAt()).isEqualTo(Instant.parse("2026-10-18T11:30:00Z"));
        assertThatThrownBy(() -> service.checkIn("bms1.9.sig", STAFF))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already used");
    }

    @Test
    void checkInRejectsForgedCode() {
        when(ticketCodes.verify("bms1.9.forged")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(SHOW_DAY).checkIn("bms1.9.forged", STAFF))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid ticket");
    }

    @Test
    void checkInRejectsWrongDay() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(confirmed()));
        Clock dayBefore = Clock.fixed(Instant.parse("2026-10-17T11:30:00Z"), ZoneId.of("Asia/Kolkata"));

        assertThatThrownBy(() -> service(dayBefore).checkIn("bms1.9.sig", STAFF))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Ticket is for 2026-10-18, not today");
    }

    @Test
    void checkInNeedsStaffOfThatCinema() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(confirmed()));
        doThrow(new ForbiddenException("You cannot manage cinema 10"))
                .when(cinemaAccessService)
                .ensureCanManage(STAFF, 10L);

        assertThatThrownBy(() -> service(SHOW_DAY).checkIn("bms1.9.sig", STAFF)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void checkInRejectsUnpaidBooking() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(BookingServiceTest.pending()));

        assertThatThrownBy(() -> service(SHOW_DAY).checkIn("bms1.9.sig", STAFF))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ticket is not valid: booking is PENDING_PAYMENT");
    }

    private TicketService service(Clock clock) {
        return new TicketService(
                bookingRepository,
                ticketCodes,
                cinemaService,
                cinemaAccessService,
                mock(BookingMemberRepository.class),
                clock);
    }

    private static Booking confirmed() {
        Booking booking = BookingServiceTest.pending();
        booking.confirm("pay_1", Instant.parse("2026-10-18T05:00:00Z"));
        return booking;
    }
}
