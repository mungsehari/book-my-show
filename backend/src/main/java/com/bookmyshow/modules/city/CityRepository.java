package com.bookmyshow.modules.city;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface CityRepository extends JpaRepository<City, Long> {

    List<City> findAllByOrderByNameAsc();
}
