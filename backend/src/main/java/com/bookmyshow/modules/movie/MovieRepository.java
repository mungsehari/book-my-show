package com.bookmyshow.modules.movie;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findAllByOrderByTitleAsc();

    List<Movie> findAllByLanguageIgnoreCaseOrderByTitleAsc(String language);

    List<Movie> findAllByIdInOrderByTitleAsc(Collection<Long> ids);
}
