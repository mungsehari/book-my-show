package com.bookmyshow.integrations.ai;

import java.util.Set;

/**
 * Movie search filters read from a free-text request. Empty sets and a null duration mean "no limit". Genre and
 * language values always come from the catalog, never made up.
 */
public record MovieFilters(
        Set<String> genres, Set<String> languages, Integer maxDurationMinutes, Set<String> certificates) {

    public static final MovieFilters NONE = new MovieFilters(Set.of(), Set.of(), null, Set.of());

    public MovieFilters {
        genres = genres == null ? Set.of() : Set.copyOf(genres);
        languages = languages == null ? Set.of() : Set.copyOf(languages);
        certificates = certificates == null ? Set.of() : Set.copyOf(certificates);
    }
}
