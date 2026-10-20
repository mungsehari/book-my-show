import { apiGet, appPost } from "@/lib/api";
import type { Review, ReviewInput, ReviewPage } from "@/modules/review/types";

export const fetchReviews = (movieId: number) =>
  apiGet<ReviewPage>(`/movies/${movieId}/reviews`, { size: 20 });

export const postReview = (movieId: number, input: ReviewInput) =>
  appPost<Review>(`/api/movies/${movieId}/reviews`, input);
