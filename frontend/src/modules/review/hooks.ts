import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchReviews, postReview } from "@/modules/review/api";
import type { ReviewInput } from "@/modules/review/types";

export const useReviews = (movieId: number) =>
  useQuery({ queryKey: ["reviews", movieId], queryFn: () => fetchReviews(movieId) });

export function usePostReview(movieId: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (input: ReviewInput) => postReview(movieId, input),
    onSuccess: () => client.invalidateQueries({ queryKey: ["reviews", movieId] }),
  });
}
