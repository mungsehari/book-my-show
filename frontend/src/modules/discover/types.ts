import type { Movie } from "@/modules/movie/types";

export type MovieFilters = {
  genres: string[];
  languages: string[];
  maxDurationMinutes: number | null;
  certificates: string[];
};

export type MoodSearchResult = {
  query: string;
  filters: MovieFilters;
  /** Where the filters came from, for example the AI or the keyword fallback. */
  source: string;
  movies: Movie[];
};
