import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { closePoll, createPoll, fetchMyPolls, fetchPoll, votePoll } from "@/modules/poll/api";
import type { Poll } from "@/modules/poll/types";

export const useMyPolls = () => useQuery({ queryKey: ["polls"], queryFn: fetchMyPolls });

/** One poll, refreshed every few seconds so votes show up live. */
export const usePoll = (id: number) =>
  useQuery({
    queryKey: ["polls", id],
    queryFn: () => fetchPoll(id),
    refetchInterval: (query) => (query.state.data?.status === "CLOSED" ? false : 4_000),
  });

export function useCreatePoll() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: createPoll,
    onSuccess: () => client.invalidateQueries({ queryKey: ["polls"] }),
  });
}

/** Vote or close: the answer is the fresh poll, so put it straight in the cache. */
function usePollAction<TInput>(call: (input: TInput) => Promise<Poll>) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: call,
    onSuccess: (poll) => {
      client.setQueryData(["polls", poll.id], poll);
      client.invalidateQueries({ queryKey: ["polls"], exact: true });
    },
  });
}

export const useVote = () =>
  usePollAction((input: { id: number; showId: number }) => votePoll(input.id, input.showId));
export const useClosePoll = () => usePollAction(closePoll);
