package com.bookmyshow.modules.screen;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ScreenViewRepository extends JpaRepository<ScreenView, Long> {

    List<ScreenView> findByScreenIdOrderByFromRowAsc(Long screenId);
}
