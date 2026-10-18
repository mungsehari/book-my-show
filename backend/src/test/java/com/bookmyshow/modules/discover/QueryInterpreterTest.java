package com.bookmyshow.modules.discover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.integrations.ai.AiQueryInterpreter;
import com.bookmyshow.integrations.ai.MovieFilters;
import com.bookmyshow.modules.discover.QueryInterpreter.Interpretation;
import com.bookmyshow.modules.movie.MovieService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

class QueryInterpreterTest {

    private static final MovieFilters COMEDY = new MovieFilters(Set.of("Comedy"), Set.of(), 120, Set.of());

    private AiQueryInterpreter ai;
    private ValueOperations<String, String> values;
    private QueryInterpreter interpreter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ai = mock(AiQueryInterpreter.class);
        MovieService movieService = mock(MovieService.class);
        when(movieService.catalogGenres()).thenReturn(List.of("Comedy", "Drama"));
        when(movieService.catalogLanguages()).thenReturn(List.of("Hindi"));
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(anyString())).thenReturn(1L);
        when(ai.isAvailable()).thenReturn(true);
        interpreter = new QueryInterpreter(
                ai,
                movieService,
                redis,
                JsonMapper.builder().build(),
                Clock.fixed(Instant.parse("2026-10-18T05:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void claudeAnswerIsUsedAndCached() {
        when(ai.interpret(eq("kuch halka funny"), anyList(), anyList())).thenReturn(Optional.of(COMEDY));

        Interpretation result = interpreter.interpret("  Kuch   HALKA funny ", "1.2.3.4");

        assertThat(result.source()).isEqualTo("claude");
        assertThat(result.filters()).isEqualTo(COMEDY);
        verify(values).set(anyString(), anyString(), eq(Duration.ofHours(24)));
    }

    @Test
    void cachedAnswerSkipsClaude() {
        when(values.get(anyString()))
                .thenReturn(
                        "{\"genres\":[\"Comedy\"],\"languages\":[],\"maxDurationMinutes\":120,\"certificates\":[]}");

        Interpretation result = interpreter.interpret("kuch halka funny", "1.2.3.4");

        assertThat(result.source()).isEqualTo("cache");
        assertThat(result.filters()).isEqualTo(COMEDY);
        verify(ai, never()).interpret(anyString(), anyList(), anyList());
    }

    @Test
    void claudeFailureFallsBackToKeywords() {
        when(ai.interpret(anyString(), anyList(), anyList())).thenReturn(Optional.empty());

        Interpretation result = interpreter.interpret("kuch halka funny", "1.2.3.4");

        assertThat(result.source()).isEqualTo("keyword");
        assertThat(result.filters().genres()).containsExactly("Comedy");
    }

    @Test
    void overRateLimitUsesKeywordsWithoutCallingClaude() {
        when(values.increment(anyString())).thenReturn(21L);

        Interpretation result = interpreter.interpret("kuch halka funny", "1.2.3.4");

        assertThat(result.source()).isEqualTo("keyword");
        verify(ai, never()).interpret(anyString(), anyList(), anyList());
    }

    @Test
    void redisDownStillAnswersWithKeywords() {
        when(values.get(anyString())).thenThrow(new IllegalStateException("redis down"));
        when(values.increment(anyString())).thenThrow(new IllegalStateException("redis down"));

        Interpretation result = interpreter.interpret("kuch halka funny", "1.2.3.4");

        assertThat(result.source()).isEqualTo("keyword");
        verify(ai, never()).interpret(anyString(), anyList(), anyList());
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
    }
}
