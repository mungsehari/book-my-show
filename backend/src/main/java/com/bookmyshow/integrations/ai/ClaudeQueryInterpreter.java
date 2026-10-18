package com.bookmyshow.integrations.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Claude reads the request and answers with JSON filters. Claude only picks filters; the movies always come from our
 * own catalog, so it can never suggest a movie we do not have. Every value it returns is checked against the catalog
 * and anything unknown is dropped. Any failure returns empty, and the caller falls back to keyword search.
 */
@Component
public class ClaudeQueryInterpreter implements AiQueryInterpreter {

    private static final Logger log = LoggerFactory.getLogger(ClaudeQueryInterpreter.class);
    private static final Set<String> CERTIFICATES = Set.of("U", "UA", "A");
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final ClaudeProperties properties;
    private final JsonMapper jsonMapper;
    private final AnthropicClient client;

    public ClaudeQueryInterpreter(ClaudeProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.client = properties.isConfigured()
                ? AnthropicOkHttpClient.builder()
                        .apiKey(properties.apiKey())
                        .timeout(TIMEOUT)
                        .maxRetries(0)
                        .build()
                : null;
    }

    @Override
    public boolean isAvailable() {
        return client != null;
    }

    @Override
    public Optional<MovieFilters> interpret(String query, List<String> catalogGenres, List<String> catalogLanguages) {
        if (client == null) {
            return Optional.empty();
        }
        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(properties.model())
                    .maxTokens(4000L)
                    // A small extraction task: low effort keeps it fast and cheap.
                    .outputConfig(OutputConfig.builder()
                            .effort(OutputConfig.Effort.LOW)
                            .build())
                    .system(systemPrompt(catalogGenres, catalogLanguages))
                    .addUserMessage(query)
                    .build();
            Message response = client.messages().create(params);
            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(textBlock -> textBlock.text())
                    .collect(Collectors.joining());
            return parse(text, catalogGenres, catalogLanguages, jsonMapper);
        } catch (RuntimeException ex) {
            log.warn("Claude query interpretation failed, falling back to keywords: {}", ex.toString());
            return Optional.empty();
        }
    }

    static String systemPrompt(List<String> genres, List<String> languages) {
        return """
                You turn a movie search request into filters. The request may be in English, Hindi, or Hinglish.
                Reply with one JSON object and nothing else, in this shape:
                {"genres": [], "languages": [], "maxDurationMinutes": null, "certificates": []}
                Rules:
                - genres: pick only from %s. "Light", "halka", or "funny" means comedy-like genres.
                - languages: pick only from %s, and only if the request names a language.
                - maxDurationMinutes: a whole number only if the request limits length ("2 ghante se kam" is 120).
                - certificates: pick from ["U", "UA", "A"] only if the request needs it ("with kids" means ["U"]).
                - Leave a field empty or null when the request says nothing about it.
                """.formatted(genres, languages);
    }

    /**
     * Reads the JSON object out of Claude's reply and keeps only known values, matched without case to the catalog.
     * Empty if there is no valid object.
     */
    static Optional<MovieFilters> parse(
            String text, List<String> catalogGenres, List<String> catalogLanguages, JsonMapper jsonMapper) {
        if (text == null) {
            return Optional.empty();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return Optional.empty();
        }
        JsonNode root;
        try {
            root = jsonMapper.readTree(text.substring(start, end + 1));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
        if (root == null || !root.isObject()) {
            return Optional.empty();
        }
        Set<String> genres = pick(root.path("genres"), canonical(catalogGenres));
        Set<String> languages = pick(root.path("languages"), canonical(catalogLanguages));
        Set<String> certificates = pick(root.path("certificates"), canonical(List.copyOf(CERTIFICATES)));
        JsonNode duration = root.path("maxDurationMinutes");
        Integer maxDuration =
                duration.isNumber() && duration.asInt() > 0 && duration.asInt() <= 600 ? duration.asInt() : null;
        return Optional.of(new MovieFilters(genres, languages, maxDuration, certificates));
    }

    private static Map<String, String> canonical(List<String> values) {
        Map<String, String> byLowerCase = new HashMap<>();
        values.forEach(value -> byLowerCase.put(value.toLowerCase(Locale.ROOT), value));
        return byLowerCase;
    }

    private static Set<String> pick(JsonNode array, Map<String, String> allowed) {
        Set<String> picked = new HashSet<>();
        if (array.isArray()) {
            for (JsonNode item : array) {
                if (item.isString()) {
                    String match = allowed.get(item.asString().strip().toLowerCase(Locale.ROOT));
                    if (match != null) {
                        picked.add(match);
                    }
                }
            }
        }
        return picked;
    }
}
