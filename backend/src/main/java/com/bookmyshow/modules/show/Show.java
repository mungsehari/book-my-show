package com.bookmyshow.modules.show;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A movie show at a cinema. movieId links to the movie catalog and is null until every show is linked. movieName stays
 * until then. screenId is null for shows created before screens existed.
 */
@Entity
@Table(name = "shows")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cinema_id", nullable = false)
    private Long cinemaId;

    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "screen_id")
    private Long screenId;

    @Column(name = "movie_name", nullable = false, length = 200)
    private String movieName;

    @Column(name = "show_date", nullable = false)
    private LocalDate showDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Calm show: no kids' crowd, quieter audience. Set by the theatre. */
    @Column(nullable = false)
    private Boolean quiet = false;

    /** Lower sound, lights partly on. */
    @Column(name = "sensory_friendly", nullable = false)
    private Boolean sensoryFriendly = false;

    @Column(nullable = false)
    private Boolean subtitles = false;

    protected Show() {}

    public Show(
            Long cinemaId,
            Long screenId,
            Long movieId,
            String movieName,
            LocalDate showDate,
            LocalTime startTime,
            BigDecimal price) {
        this.cinemaId = cinemaId;
        this.screenId = screenId;
        this.movieId = movieId;
        this.movieName = movieName;
        this.showDate = showDate;
        this.startTime = startTime;
        this.price = price;
    }

    void setFeatures(boolean quiet, boolean sensoryFriendly, boolean subtitles) {
        this.quiet = quiet;
        this.sensoryFriendly = sensoryFriendly;
        this.subtitles = subtitles;
    }

    public boolean isQuiet() {
        return Boolean.TRUE.equals(quiet);
    }

    public boolean isSensoryFriendly() {
        return Boolean.TRUE.equals(sensoryFriendly);
    }

    public boolean hasSubtitles() {
        return Boolean.TRUE.equals(subtitles);
    }

    public Long getId() {
        return id;
    }

    public Long getCinemaId() {
        return cinemaId;
    }

    public Long getScreenId() {
        return screenId;
    }

    public Long getMovieId() {
        return movieId;
    }

    public String getMovieName() {
        return movieName;
    }

    public LocalDate getShowDate() {
        return showDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
