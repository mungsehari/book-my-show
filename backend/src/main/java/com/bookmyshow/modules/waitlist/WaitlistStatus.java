package com.bookmyshow.modules.waitlist;

import java.util.List;

/** Waitlist states. Stored as text in waitlist_entries.status. */
public final class WaitlistStatus {

    public static final String WAITING = "WAITING";
    /** Seats are held for the user for a few minutes. */
    public static final String OFFERED = "OFFERED";

    public static final String BOOKED = "BOOKED";
    /** The user did not book in time. */
    public static final String EXPIRED = "EXPIRED";

    public static final String LEFT = "LEFT";

    static final List<String> ACTIVE = List.of(WAITING, OFFERED);

    private WaitlistStatus() {}
}
