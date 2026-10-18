package com.bookmyshow.modules.alert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * "Tell me when a ticket for this movie in this city costs maxPrice or less." ACTIVE until it fires once (TRIGGERED)
 * or the user stops it (CANCELLED). Stores IDs only, so modules stay independent.
 */
@Entity
@Table(name = "price_alerts")
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(name = "city_id", nullable = false)
    private Long cityId;

    @Column(name = "max_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxPrice;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "triggered_at")
    private Instant triggeredAt;

    @Column(name = "triggered_show_id")
    private Long triggeredShowId;

    @Column(name = "triggered_price", precision = 10, scale = 2)
    private BigDecimal triggeredPrice;

    protected PriceAlert() {}

    public PriceAlert(Long userId, Long movieId, Long cityId, BigDecimal maxPrice, Instant createdAt) {
        this.userId = userId;
        this.movieId = movieId;
        this.cityId = cityId;
        this.maxPrice = maxPrice;
        this.createdAt = createdAt;
        this.status = PriceAlertStatus.ACTIVE;
    }

    void trigger(long showId, BigDecimal price, Instant now) {
        this.status = PriceAlertStatus.TRIGGERED;
        this.triggeredShowId = showId;
        this.triggeredPrice = price;
        this.triggeredAt = now;
    }

    void cancel() {
        this.status = PriceAlertStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getMovieId() {
        return movieId;
    }

    public Long getCityId() {
        return cityId;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public Long getTriggeredShowId() {
        return triggeredShowId;
    }

    public BigDecimal getTriggeredPrice() {
        return triggeredPrice;
    }
}
