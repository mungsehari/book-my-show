package com.bookmyshow.modules.alert;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

/** All request and response types for the alert module. */
public final class PriceAlertTypes {

    private PriceAlertTypes() {}

    public record CreatePriceAlertRequest(
            @NotNull @Positive Long movieId,
            @NotNull @Positive Long cityId,

            @NotNull @DecimalMin("1.00") @Digits(integer = 8, fraction = 2)
            BigDecimal maxPrice) {}

    /** When TRIGGERED, triggeredShowId is the show that matched, at triggeredPrice. */
    public record PriceAlertResponse(
            Long id,
            Long movieId,
            Long cityId,
            BigDecimal maxPrice,
            String status,
            Instant createdAt,
            Instant triggeredAt,
            Long triggeredShowId,
            BigDecimal triggeredPrice) {

        static PriceAlertResponse from(PriceAlert alert) {
            return new PriceAlertResponse(
                    alert.getId(),
                    alert.getMovieId(),
                    alert.getCityId(),
                    alert.getMaxPrice(),
                    alert.getStatus(),
                    alert.getCreatedAt(),
                    alert.getTriggeredAt(),
                    alert.getTriggeredShowId(),
                    alert.getTriggeredPrice());
        }
    }
}
