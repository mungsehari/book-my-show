import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchMyWaitlist, joinWaitlist, leaveWaitlist } from "@/modules/waitlist/api";

const key = (showId: number) => ["waitlist", showId];

/** Your entry, checked every few seconds so a new offer shows up. */
export const useMyWaitlist = (showId: number) =>
  useQuery({
    queryKey: key(showId),
    queryFn: () => fetchMyWaitlist(showId),
    refetchInterval: 5_000,
  });

export function useJoinWaitlist(showId: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (seats: number) => joinWaitlist(showId, seats),
    onSuccess: (entry) => client.setQueryData(key(showId), entry),
  });
}

export function useLeaveWaitlist(showId: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: leaveWaitlist,
    onSuccess: () => client.setQueryData(key(showId), null),
  });
}
