package com.bookmyshow.modules.show;

import com.bookmyshow.modules.show.ShowTypes.DealInfo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Last-minute deal rule. Pure functions, no database.
 *
 * <p>In the last {@link #DEAL_WINDOW} before a show starts, if it is less than {@link #MAX_PERCENT_FULL}% full, seats
 * cost {@link #PERCENT_OFF}% less. Theatres can turn this off per show.
 */
final class ShowPricing {

    static final Duration DEAL_WINDOW = Duration.ofMinutes(60);
    static final int MAX_PERCENT_FULL = 70;
    static final int PERCENT_OFF = 20;

    private ShowPricing() {}

    /** The deal running now, or null. */
    static DealInfo dealFor(Show show, LocalDateTime now, int percentFull) {
        LocalDateTime start = LocalDateTime.of(show.getShowDate(), show.getStartTime());
        boolean inWindow = now.isBefore(start) && !now.isBefore(start.minus(DEAL_WINDOW));
        if (!show.isDealsEnabled() || !inWindow || percentFull >= MAX_PERCENT_FULL) {
            return null;
        }
        return new DealInfo(PERCENT_OFF, start);
    }

    /** The price after the deal, rounded to whole rupees. */
    static BigDecimal apply(BigDecimal price, DealInfo deal) {
        if (deal == null || price == null) {
            return price;
        }
        return price.multiply(BigDecimal.valueOf(100 - deal.percentOff()))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.UNNECESSARY);
    }
}
