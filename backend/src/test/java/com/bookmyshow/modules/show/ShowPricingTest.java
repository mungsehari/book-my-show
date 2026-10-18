package com.bookmyshow.modules.show;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookmyshow.modules.show.ShowTypes.DealInfo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ShowPricingTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 20, 18, 0);

    @Test
    void dealInLastHourWhenShowIsNotFull() {
        DealInfo deal = ShowPricing.dealFor(show(true), START.minusMinutes(30), 50);

        assertThat(deal).isNotNull();
        assertThat(deal.percentOff()).isEqualTo(20);
        assertThat(deal.endsAt()).isEqualTo(START);
    }

    @Test
    void dealStartsExactlyOneHourBefore() {
        assertThat(ShowPricing.dealFor(show(true), START.minusMinutes(60), 0)).isNotNull();
        assertThat(ShowPricing.dealFor(show(true), START.minusMinutes(61), 0)).isNull();
    }

    @Test
    void noDealOnceShowStarts() {
        assertThat(ShowPricing.dealFor(show(true), START, 0)).isNull();
        assertThat(ShowPricing.dealFor(show(true), START.plusMinutes(5), 0)).isNull();
    }

    @Test
    void noDealWhenSeventyPercentFull() {
        assertThat(ShowPricing.dealFor(show(true), START.minusMinutes(30), 69)).isNotNull();
        assertThat(ShowPricing.dealFor(show(true), START.minusMinutes(30), 70)).isNull();
    }

    @Test
    void noDealWhenTheatreTurnedItOff() {
        assertThat(ShowPricing.dealFor(show(false), START.minusMinutes(30), 0)).isNull();
    }

    @Test
    void roundsToWholeRupees() {
        DealInfo deal = new DealInfo(20, START);

        assertThat(ShowPricing.apply(new BigDecimal("249.00"), deal)).isEqualByComparingTo("199.00");
        assertThat(ShowPricing.apply(new BigDecimal("225.00"), deal)).isEqualByComparingTo("180.00");
        assertThat(ShowPricing.apply(new BigDecimal("200.00"), null)).isEqualByComparingTo("200.00");
    }

    private static Show show(boolean dealsEnabled) {
        Show show = new Show(
                10L, 3L, 2L, "Sky Riders", LocalDate.of(2026, 10, 20), LocalTime.of(18, 0), new BigDecimal("200.00"));
        show.setDealsEnabled(dealsEnabled);
        return show;
    }
}
