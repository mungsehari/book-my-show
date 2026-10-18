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
 * until then.
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

    @Column(name = "movie_name", nullable = false, length = 200)
    private String movieName;

    @Column(name = "show_date", nullable = false)
    private LocalDate showDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    protected Show() {}

    public Show(
            Long cinemaId, Long movieId, String movieName, LocalDate showDate, LocalTime startTime, BigDecimal price) {
        this.cinemaId = cinemaId;
        this.movieId = movieId;
        this.movieName = movieName;
        this.showDate = showDate;
        this.startTime = startTime;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public Long getCinemaId() {
        return cinemaId;
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
