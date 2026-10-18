package com.bookmyshow.modules.movie;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findAllByOrderByTitleAsc();

    List<Movie> findAllByLanguageIgnoreCaseOrderByTitleAsc(String language);

    List<Movie> findAllByIdInOrderByTitleAsc(Collection<Long> ids);

    boolean existsByTitleIgnoreCaseAndLanguageIgnoreCase(String title, String language);

    @Query("select distinct m.genre from Movie m order by m.genre")
    List<String> findDistinctGenres();

    @Query("select distinct m.language from Movie m order by m.language")
    List<String> findDistinctLanguages();
}
