import { apiGet, type ListResponse } from "@/lib/api";
import type { Movie } from "@/modules/movie/types";

export const fetchMoviesInCity = (cityId: number, date?: string) =>
  apiGet<ListResponse<Movie>>(`/cities/${cityId}/movies`, { date });

export const fetchMovie = (id: number) => apiGet<Movie>(`/movies/${id}`);
