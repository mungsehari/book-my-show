import { useQuery } from "@tanstack/react-query";
import { searchByMood } from "@/modules/discover/api";

/** Runs the search for a submitted text. Does nothing while the text is empty. */
export const useMoodSearch = (query: string, cityId: number | null) =>
  useQuery({
    queryKey: ["mood-search", query, cityId],
    queryFn: () => searchByMood(query, cityId),
    enabled: query.length > 0,
    staleTime: 5 * 60_000,
  });
