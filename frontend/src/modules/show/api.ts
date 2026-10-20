import { apiGet, type ListResponse } from "@/lib/api";
import type { Show } from "@/modules/show/types";

export const fetchShowsInCinema = (cinemaId: number, date: string) =>
  apiGet<ListResponse<Show>>(`/cinemas/${cinemaId}/shows`, { date });
