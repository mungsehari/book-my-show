package com.bookmyshow.modules.screen;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A photo of the screen as seen from rows fromRow to toRow. The image is hosted elsewhere; only its URL is stored. */
@Entity
@Table(name = "screen_views")
public class ScreenView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "screen_id", nullable = false)
    private Long screenId;

    @Column(name = "from_row", nullable = false, length = 1)
    private String fromRow;

    @Column(name = "to_row", nullable = false, length = 1)
    private String toRow;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(length = 200)
    private String caption;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ScreenView() {}

    public ScreenView(Long screenId, String fromRow, String toRow, String imageUrl, String caption, Instant createdAt) {
        this.screenId = screenId;
        this.fromRow = fromRow;
        this.toRow = toRow;
        this.imageUrl = imageUrl;
        this.caption = caption;
        this.createdAt = createdAt;
    }

    /** True if the two row ranges share at least one row. */
    boolean overlaps(String otherFrom, String otherTo) {
        return otherFrom.compareTo(toRow) <= 0 && fromRow.compareTo(otherTo) <= 0;
    }

    public Long getId() {
        return id;
    }

    public Long getScreenId() {
        return screenId;
    }

    public String getFromRow() {
        return fromRow;
    }

    public String getToRow() {
        return toRow;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCaption() {
        return caption;
    }
}
