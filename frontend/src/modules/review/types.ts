export type Review = {
  id: number;
  movieId: number;
  userName: string;
  rating: number;
  text: string | null;
  verified: boolean;
  createdAt: string;
};

export type ReviewPage = {
  movieId: number;
  averageRating: number | null;
  reviewCount: number;
  items: Review[];
  page: number;
  size: number;
  totalPages: number;
};

export type ReviewInput = { rating: number; text?: string };
