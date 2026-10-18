package com.bookmyshow.modules.discover;

import com.bookmyshow.integrations.ai.MovieFilters;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The fallback reader for mood search: plain keyword rules for English and Hinglish. Pure functions, no database. Only
 * genres and languages that exist in the catalog come out.
 */
final class KeywordQueryParser {

    /** Lower-case genre name to the words that mean it. A catalog genre not listed here matches its own name. */
    private static final Map<String, List<String>> GENRE_WORDS = Map.ofEntries(
            Map.entry(
                    "comedy",
                    List.of("comedy", "funny", "hasi", "hasna", "hasaane", "mazedaar", "halka", "light", "laugh")),
            Map.entry("drama", List.of("drama", "emotional", "serious")),
            Map.entry("action", List.of("action", "fight", "maar", "dhamaka", "dhamakedaar")),
            Map.entry("thriller", List.of("thriller", "suspense", "mystery", "rahasya")),
            Map.entry("horror", List.of("horror", "darr", "darawni", "bhoot", "scary")),
            Map.entry("romance", List.of("romance", "romantic", "pyaar", "love", "ishq")),
            Map.entry("family", List.of("family", "parivaar", "kids", "bachche", "bachchon", "children")),
            Map.entry("animation", List.of("animation", "animated", "cartoon")),
            Map.entry("sci-fi", List.of("sci-fi", "scifi", "science fiction", "space")));

    /** "Light" also allows these genres, when the catalog has them. */
    private static final List<String> LIGHT_WORDS = List.of("halka", "light", "chill");

    private static final List<String> LIGHT_GENRES = List.of("comedy", "family", "animation");

    private static final Map<String, List<String>> LANGUAGE_WORDS = Map.of(
            "hindi", List.of("hindi"),
            "english", List.of("english", "angrezi", "hollywood"),
            "tamil", List.of("tamil"),
            "telugu", List.of("telugu"));

    private static final List<String> KIDS_WORDS = List.of("kids", "bachche", "bachchon", "children", "family");

    private static final Pattern HOURS =
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:ghante|ghanta|hours|hour|hrs|hr|h)\\b");
    private static final Pattern MINUTES = Pattern.compile("(\\d{2,3})\\s*(?:minutes|minute|mins|min)\\b");

    private KeywordQueryParser() {}

    static MovieFilters parse(String query, List<String> catalogGenres, List<String> catalogLanguages) {
        String text = " " + query.toLowerCase(Locale.ROOT) + " ";
        Set<String> genres = new HashSet<>();
        for (String genre : catalogGenres) {
            String key = genre.toLowerCase(Locale.ROOT);
            List<String> words = GENRE_WORDS.getOrDefault(key, List.of(key));
            if (mentionsAny(text, words) || (LIGHT_GENRES.contains(key) && mentionsAny(text, LIGHT_WORDS))) {
                genres.add(genre);
            }
        }
        Set<String> languages = new HashSet<>();
        for (String language : catalogLanguages) {
            String key = language.toLowerCase(Locale.ROOT);
            if (mentionsAny(text, LANGUAGE_WORDS.getOrDefault(key, List.of(key)))) {
                languages.add(language);
            }
        }
        Set<String> certificates = mentionsAny(text, KIDS_WORDS) ? Set.of("U") : Set.of();
        return new MovieFilters(genres, languages, maxDuration(text), certificates);
    }

    /** "2 ghante", "1.5 hours", "dedh ghanta", "100 min". Any length mentioned is read as a limit. */
    static Integer maxDuration(String text) {
        if (text.contains("dedh")) {
            return 90;
        }
        if (text.contains("dhai")) {
            return 150;
        }
        Matcher hours = HOURS.matcher(text);
        if (hours.find()) {
            int minutes = (int) Math.round(Double.parseDouble(hours.group(1)) * 60);
            return minutes > 0 && minutes <= 600 ? minutes : null;
        }
        Matcher minutes = MINUTES.matcher(text);
        if (minutes.find()) {
            int value = Integer.parseInt(minutes.group(1));
            return value > 0 && value <= 600 ? value : null;
        }
        return null;
    }

    private static boolean mentionsAny(String text, List<String> words) {
        for (String word : words) {
            if (Pattern.compile("(?<![a-z])" + Pattern.quote(word) + "(?![a-z])")
                    .matcher(text)
                    .find()) {
                return true;
            }
        }
        return false;
    }
}
