import { useQueries } from "@tanstack/react-query";
import { fetchShowsInCinema } from "@/modules/show/api";
import type { Show } from "@/modules/show/types";

export type CinemaShows = {
  cinemaId: number;
  shows: Show[];
};

/** Shows of one movie on one date, grouped by cinema. */
export function useMovieShows(cinemaIds: number[], movieId: number, date: string) {
  return useQueries({
    queries: cinemaIds.map((cinemaId) => ({
      queryKey: ["shows", "cinema", cinemaId, date],
      queryFn: () => fetchShowsInCinema(cinemaId, date),
      select: (data: { items: Show[] }): CinemaShows => ({
        cinemaId,
        shows: data.items.filter((show) => show.movieId === movieId),
      }),
    })),
    combine: (results) => ({
      isLoading: results.some((result) => result.isLoading),
      error: results.find((result) => result.error)?.error ?? null,
      groups: results
        .map((result) => result.data)
        .filter((group): group is CinemaShows => !!group && group.shows.length > 0),
    }),
  });
}

/** Every show in the given cinemas on one date, soonest first. */
export function useShowsOnDate(cinemaIds: number[], date: string) {
  return useQueries({
    queries: cinemaIds.map((cinemaId) => ({
      queryKey: ["shows", "cinema", cinemaId, date],
      queryFn: () => fetchShowsInCinema(cinemaId, date),
    })),
    combine: (results) => ({
      isLoading: results.some((result) => result.isLoading),
      shows: results
        .flatMap((result) => result.data?.items ?? [])
        .sort((a, b) => a.startTime.localeCompare(b.startTime)),
    }),
  });
}
