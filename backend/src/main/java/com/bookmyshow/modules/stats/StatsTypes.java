package com.bookmyshow.modules.stats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** All response types for the admin stats module. Money is in the booking currency, only confirmed sales count. */
public final class StatsTypes {

    private StatsTypes() {}

    /**
     * Numbers for the last {@code days} days, today included. refundsPending is not limited by days: it is every
     * paid booking still waiting for its money back.
     */
    public record SummaryResponse(
            int days,
            long bookings,
            long ticketsSold,
            BigDecimal revenue,
            BigDecimal averageTicketPrice,
            long cancelled,
            long refundsPending) {}

    /** One day of sales. Days with no sales are still listed, with zeros. */
    public record DailyPoint(LocalDate date, long bookings, long tickets, BigDecimal revenue) {}

    public record DailyResponse(int days, List<DailyPoint> points) {}

    public record TopMovie(String movieName, long tickets, BigDecimal revenue) {}

    public record TopMoviesResponse(int days, List<TopMovie> items) {}

    /** level is LOW, MEDIUM, HIGH, or HOUSEFULL, same as the crowd meter. */
    public record ShowOccupancy(
            Long showId,
            Long cinemaId,
            String movieName,
            LocalDate showDate,
            LocalTime startTime,
            int totalSeats,
            int sold,
            int held,
            int percentFull,
            String level) {}

    public record OccupancyResponse(LocalDate date, List<ShowOccupancy> items) {}
}
