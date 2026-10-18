package com.bookmyshow.modules.cinema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A cinema in a city. Stores the city ID only, so modules stay independent. */
@Entity
@Table(name = "cinemas")
public class Cinema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_id", nullable = false)
    private Long cityId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    protected Cinema() {}

    public Cinema(Long cityId, String name, String address) {
        this.cityId = cityId;
        this.name = name;
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public Long getCityId() {
        return cityId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}
