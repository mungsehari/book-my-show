import { apiGet } from "@/lib/api";
import type { MoodSearchResult } from "@/modules/discover/types";

export const searchByMood = (q: string, cityId: number | null) =>
  apiGet<MoodSearchResult>("/movies/search", { q, cityId });
