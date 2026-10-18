package com.bookmyshow.modules.booking;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.booking.BookingTypes.TicketResponse;
import com.bookmyshow.modules.cinema.CinemaAccessService;
import com.bookmyshow.modules.cinema.CinemaService;
import com.bookmyshow.security.CurrentUser;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tickets for confirmed bookings, and the one-time entry scan at the cinema. */
@Service
@Transactional(readOnly = true)
public class TicketService {

    private static final String INVALID = "Invalid ticket";

    private final BookingRepository bookingRepository;
    private final TicketCodes ticketCodes;
    private final CinemaService cinemaService;
    private final CinemaAccessService cinemaAccessService;
    private final BookingMemberRepository memberRepository;
    private final Clock clock;

    public TicketService(
            BookingRepository bookingRepository,
            TicketCodes ticketCodes,
            CinemaService cinemaService,
            CinemaAccessService cinemaAccessService,
            BookingMemberRepository memberRepository,
            Clock clock) {
        this.bookingRepository = bookingRepository;
        this.ticketCodes = ticketCodes;
        this.cinemaService = cinemaService;
        this.cinemaAccessService = cinemaAccessService;
        this.memberRepository = memberRepository;
        this.clock = clock;
    }

    /** The owner, or a friend in a split booking, can see the ticket. */
    public TicketResponse getTicket(long bookingId, CurrentUser user) {
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId + " not found"));
        boolean member = booking.isSplit() && memberRepository.existsByBookingIdAndUserId(bookingId, user.id());
        if (booking.getUserId() != user.id() && !member) {
            throw new ForbiddenException("This booking belongs to another user");
        }
        if (!BookingStatus.CONFIRMED.equals(booking.getStatus())) {
            throw new ConflictException(
                    "Booking " + bookingId + " is " + booking.getStatus() + ", so it has no ticket");
        }
        return toTicket(booking);
    }

    /**
     * Scans a ticket at the entry. Works once, only on the show day, and only for staff of that cinema. The booking row
     * is locked, so two scanners cannot both let the same ticket in.
     */
    @Transactional
    public TicketResponse checkIn(String ticketCode, CurrentUser staff) {
        long bookingId = ticketCodes.verify(ticketCode).orElseThrow(() -> new BadRequestException(INVALID));
        Booking booking =
                bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new BadRequestException(INVALID));
        cinemaAccessService.ensureCanManage(staff, booking.getCinemaId());
        if (!BookingStatus.CONFIRMED.equals(booking.getStatus())) {
            throw new ConflictException("Ticket is not valid: booking is " + booking.getStatus());
        }
        LocalDate today = LocalDate.now(clock);
        if (!booking.getShowDate().equals(today)) {
            throw new BadRequestException("Ticket is for " + booking.getShowDate() + ", not today");
        }
        if (booking.getCheckedInAt() != null) {
            throw new ConflictException("Ticket was already used at " + booking.getCheckedInAt());
        }
        booking.checkIn(staff.id(), clock.instant());
        return toTicket(booking);
    }

    private TicketResponse toTicket(Booking booking) {
        return new TicketResponse(
                booking.getId(),
                ticketCodes.issue(booking.getId()),
                booking.getMovieName(),
                cinemaService.getCinema(booking.getCinemaId()).name(),
                booking.getScreenName(),
                booking.getShowDate(),
                booking.getStartTime(),
                booking.getSeatLabels(),
                booking.getAmount(),
                booking.getCurrency(),
                booking.getCheckedInAt());
    }
}
