package com.bookmyshow.integrations.ai;

import java.util.List;
import java.util.Optional;

/** Reads a free-text movie request ("kuch halka funny, 2 ghante se kam") into filters. Modules use this interface only. */
public interface AiQueryInterpreter {

    /** False when no API key is set. */
    boolean isAvailable();

    /**
     * The filters, limited to the given catalog genres and languages. Empty if the call fails, times out, is refused, or
     * returns something that is not valid filters. Never throws for those cases, so callers can fall back.
     */
    Optional<MovieFilters> interpret(String query, List<String> catalogGenres, List<String> catalogLanguages);
}
