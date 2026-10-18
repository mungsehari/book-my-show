package com.bookmyshow.modules.alert;

/** Price alert states. Stored as text in price_alerts.status. */
public final class PriceAlertStatus {

    public static final String ACTIVE = "ACTIVE";
    /** Fired once. Not checked again. */
    public static final String TRIGGERED = "TRIGGERED";

    public static final String CANCELLED = "CANCELLED";

    private PriceAlertStatus() {}
}
