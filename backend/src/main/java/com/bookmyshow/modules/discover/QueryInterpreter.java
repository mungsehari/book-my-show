package com.bookmyshow.modules.discover;

import com.bookmyshow.integrations.ai.AiQueryInterpreter;
import com.bookmyshow.integrations.ai.MovieFilters;
import com.bookmyshow.modules.movie.MovieService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns a mood request into filters. Claude first, keywords as the fallback. Claude calls cost money and the search API
 * is public, so two guards sit in front: answers are cached per query for a day, and each client may make at most
 * {@link #AI_CALLS_PER_MINUTE} new Claude calls a minute (more fall back to keywords, never to an error).
 */
@Component
class QueryInterpreter {

    static final int AI_CALLS_PER_MINUTE = 20;
    static final Duration CACHE_TTL = Duration.ofHours(24);

    private static final Logger log = LoggerFactory.getLogger(QueryInterpreter.class);

    private final AiQueryInterpreter ai;
    private final MovieService movieService;
    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    QueryInterpreter(
            AiQueryInterpreter ai,
            MovieService movieService,
            StringRedisTemplate redis,
            JsonMapper jsonMapper,
            Clock clock) {
        this.ai = ai;
        this.movieService = movieService;
        this.redis = redis;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    /** source is "claude", "cache", or "keyword". */
    record Interpretation(MovieFilters filters, String source) {}

    Interpretation interpret(String query, String clientKey) {
        String normalized = query.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        List<String> genres = movieService.catalogGenres();
        List<String> languages = movieService.catalogLanguages();

        Optional<MovieFilters> cached = readCache(normalized);
        if (cached.isPresent()) {
            return new Interpretation(cached.get(), "cache");
        }
        if (ai.isAvailable() && underRateLimit(clientKey)) {
            Optional<MovieFilters> fromAi = ai.interpret(normalized, genres, languages);
            if (fromAi.isPresent()) {
                writeCache(normalized, fromAi.get());
                return new Interpretation(fromAi.get(), "claude");
            }
        }
        return new Interpretation(KeywordQueryParser.parse(normalized, genres, languages), "keyword");
    }

    private boolean underRateLimit(String clientKey) {
        try {
            long minute = clock.instant().getEpochSecond() / 60;
            String key = "ratelimit:ai:" + clientKey + ":" + minute;
            Long calls = redis.opsForValue().increment(key);
            if (calls != null && calls == 1) {
                redis.expire(key, Duration.ofSeconds(90));
            }
            return calls != null && calls <= AI_CALLS_PER_MINUTE;
        } catch (RuntimeException ex) {
            // Without Redis we cannot count calls, so play safe and skip Claude.
            log.warn("Rate limit check failed, using keyword search: {}", ex.toString());
            return false;
        }
    }

    private Optional<MovieFilters> readCache(String normalized) {
        try {
            String json = redis.opsForValue().get(cacheKey(normalized));
            return json == null ? Optional.empty() : Optional.of(jsonMapper.readValue(json, MovieFilters.class));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private void writeCache(String normalized, MovieFilters filters) {
        try {
            redis.opsForValue().set(cacheKey(normalized), jsonMapper.writeValueAsString(filters), CACHE_TTL);
        } catch (RuntimeException ex) {
            log.warn("Could not cache mood search filters: {}", ex.toString());
        }
    }

    private static String cacheKey(String normalized) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            return "moodsearch:v1:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
