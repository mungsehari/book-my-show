import { useQuery } from "@tanstack/react-query";
import { fetchMovie, fetchMoviesInCity } from "@/modules/movie/api";

export const useMoviesInCity = (cityId: number | null, date?: string) =>
  useQuery({
    queryKey: ["movies", "city", cityId, date],
    queryFn: () => fetchMoviesInCity(cityId as number, date),
    enabled: cityId !== null,
  });

export const useMovie = (id: number) =>
  useQuery({ queryKey: ["movies", id], queryFn: () => fetchMovie(id) });
