package com.bookmyshow.modules.show;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ShowPriceRepository extends JpaRepository<ShowPrice, Long> {

    List<ShowPrice> findAllByShowId(Long showId);
}
