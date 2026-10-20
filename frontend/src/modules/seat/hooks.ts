import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchSeatMap, lockSeats, releaseLock } from "@/modules/seat/api";

const POLL_MS = 5_000;

/** The seat map, refreshed every few seconds so other people's holds show up. */
export const useSeatMap = (showId: number) =>
  useQuery({
    queryKey: ["seat-map", showId],
    queryFn: () => fetchSeatMap(showId),
    refetchInterval: POLL_MS,
    staleTime: 0,
  });

export function useLockSeats(showId: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (seatIds: number[]) => lockSeats(showId, seatIds),
    onSettled: () => client.invalidateQueries({ queryKey: ["seat-map", showId] }),
  });
}

export function useReleaseLock(showId: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: releaseLock,
    onSettled: () => client.invalidateQueries({ queryKey: ["seat-map", showId] }),
  });
}
