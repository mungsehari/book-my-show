package com.bookmyshow.modules.show;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Price of one seat category (REGULAR, PREMIUM, RECLINER) for one show. */
@Entity
@Table(name = "show_prices")
public class ShowPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    protected ShowPrice() {}

    public ShowPrice(Long showId, String category, BigDecimal price) {
        this.showId = showId;
        this.category = category;
        this.price = price;
    }

    public Long getShowId() {
        return showId;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
