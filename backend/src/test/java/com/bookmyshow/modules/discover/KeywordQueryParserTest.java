package com.bookmyshow.modules.discover;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookmyshow.integrations.ai.MovieFilters;
import java.util.List;
import org.junit.jupiter.api.Test;

class KeywordQueryParserTest {

    private static final List<String> GENRES = List.of("Action", "Comedy", "Drama", "Family", "Thriller");
    private static final List<String> LANGUAGES = List.of("English", "Hindi");

    @Test
    void halkaFunnyUnderTwoHours() {
        MovieFilters filters = KeywordQueryParser.parse("kuch halka funny, 2 ghante se kam", GENRES, LANGUAGES);

        assertThat(filters.genres()).containsExactlyInAnyOrder("Comedy", "Family");
        assertThat(filters.maxDurationMinutes()).isEqualTo(120);
        assertThat(filters.languages()).isEmpty();
    }

    @Test
    void languageAndKids() {
        MovieFilters filters = KeywordQueryParser.parse("bachchon ke saath hindi movie", GENRES, LANGUAGES);

        assertThat(filters.languages()).containsExactly("Hindi");
        assertThat(filters.certificates()).containsExactly("U");
    }

    @Test
    void englishWordsAndMinutes() {
        MovieFilters filters = KeywordQueryParser.parse("a suspense thriller under 100 min", GENRES, LANGUAGES);

        assertThat(filters.genres()).containsExactly("Thriller");
        assertThat(filters.maxDurationMinutes()).isEqualTo(100);
    }

    @Test
    void hinglishDurations() {
        assertThat(KeywordQueryParser.maxDuration(" dedh ghante ki ")).isEqualTo(90);
        assertThat(KeywordQueryParser.maxDuration(" 2.5 hours ")).isEqualTo(150);
        assertThat(KeywordQueryParser.maxDuration(" no limit ")).isNull();
    }

    @Test
    void genresNotInCatalogNeverComeOut() {
        MovieFilters filters = KeywordQueryParser.parse("scary horror bhoot wali", GENRES, LANGUAGES);

        assertThat(filters.genres()).isEmpty();
    }

    @Test
    void wordsInsideOtherWordsDoNotMatch() {
        // "lightning" contains "light" but is not a request for something light.
        assertThat(KeywordQueryParser.parse("lightning", GENRES, LANGUAGES).genres())
                .isEmpty();
    }
}
