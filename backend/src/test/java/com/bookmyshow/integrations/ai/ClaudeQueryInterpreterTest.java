package com.bookmyshow.integrations.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ClaudeQueryInterpreterTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final List<String> GENRES = List.of("Action", "Comedy", "Drama");
    private static final List<String> LANGUAGES = List.of("English", "Hindi");

    @Test
    void readsValidAnswer() {
        String reply = """
                {"genres": ["comedy"], "languages": ["Hindi"], "maxDurationMinutes": 120, "certificates": ["U"]}
                """;

        MovieFilters filters =
                ClaudeQueryInterpreter.parse(reply, GENRES, LANGUAGES, JSON).orElseThrow();

        assertThat(filters.genres()).containsExactly("Comedy");
        assertThat(filters.languages()).containsExactly("Hindi");
        assertThat(filters.maxDurationMinutes()).isEqualTo(120);
        assertThat(filters.certificates()).containsExactly("U");
    }

    @Test
    void dropsValuesNotInCatalog() {
        String reply = """
                Here you go: {"genres": ["Comedy", "Romcom"], "languages": ["Klingon"],
                 "maxDurationMinutes": 9999, "certificates": ["R"]}
                """;

        MovieFilters filters =
                ClaudeQueryInterpreter.parse(reply, GENRES, LANGUAGES, JSON).orElseThrow();

        assertThat(filters.genres()).containsExactly("Comedy");
        assertThat(filters.languages()).isEmpty();
        assertThat(filters.maxDurationMinutes()).isNull();
        assertThat(filters.certificates()).isEmpty();
    }

    @Test
    void junkIsEmpty() {
        assertThat(ClaudeQueryInterpreter.parse("Sorry, I can't help.", GENRES, LANGUAGES, JSON))
                .isEmpty();
        assertThat(ClaudeQueryInterpreter.parse("{not json}", GENRES, LANGUAGES, JSON))
                .isEmpty();
        assertThat(ClaudeQueryInterpreter.parse(null, GENRES, LANGUAGES, JSON)).isEmpty();
    }

    @Test
    void offWithoutKey() {
        ClaudeQueryInterpreter interpreter = new ClaudeQueryInterpreter(new ClaudeProperties("", null), JSON);

        assertThat(interpreter.isAvailable()).isFalse();
        assertThat(interpreter.interpret("funny", GENRES, LANGUAGES)).isEmpty();
    }

    @Test
    void defaultModelIsOpus() {
        assertThat(new ClaudeProperties("key", " ").model()).isEqualTo("claude-opus-5-5");
        assertThat(new ClaudeProperties("key", "claude-haiku-5-5").model()).isEqualTo("claude-haiku-5-5");
    }

    @Test
    void promptListsOnlyCatalogValues() {
        String prompt = ClaudeQueryInterpreter.systemPrompt(GENRES, LANGUAGES);

        assertThat(prompt).contains("[Action, Comedy, Drama]").contains("[English, Hindi]");
    }
}
