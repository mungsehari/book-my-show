import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  answerRequest,
  fetchActivity,
  fetchFriends,
  fetchPrivacy,
  fetchRequests,
  removeFriend,
  savePrivacy,
  sendRequest,
} from "@/modules/friend/api";

export const useFriends = () => useQuery({ queryKey: ["friends"], queryFn: fetchFriends });

export const useFriendRequests = () =>
  useQuery({ queryKey: ["friends", "requests"], queryFn: fetchRequests });

export const useFriendsActivity = (movieId: number, enabled: boolean) =>
  useQuery({
    queryKey: ["friends", "activity", movieId],
    queryFn: () => fetchActivity(movieId),
    enabled,
    retry: false,
  });

export const usePrivacy = () => useQuery({ queryKey: ["privacy"], queryFn: fetchPrivacy });

/** Mutation that refreshes every friends list after it succeeds. */
function useFriendMutation<TInput, TResult>(call: (input: TInput) => Promise<TResult>) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: call,
    onSuccess: () => client.invalidateQueries({ queryKey: ["friends"] }),
  });
}

export const useSendRequest = () => useFriendMutation(sendRequest);
export const useRemoveFriend = () => useFriendMutation(removeFriend);
export const useAnswerRequest = () =>
  useFriendMutation((input: { id: number; action: "accept" | "decline" }) =>
    answerRequest(input.id, input.action),
  );

export function useSavePrivacy() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: savePrivacy,
    onSuccess: (privacy) => client.setQueryData(["privacy"], privacy),
  });
}
