import { apiGet, type ListResponse } from "@/lib/api";
import type { Cinema } from "@/modules/cinema/types";

export const fetchCinemasInCity = (cityId: number) =>
  apiGet<ListResponse<Cinema>>(`/cities/${cityId}/cinemas`);
