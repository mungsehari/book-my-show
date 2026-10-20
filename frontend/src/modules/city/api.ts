import { apiGet, type ListResponse } from "@/lib/api";
import type { City } from "@/modules/city/types";

export const fetchCities = () => apiGet<ListResponse<City>>("/cities");
