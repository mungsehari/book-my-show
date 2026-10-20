export type PriceAlert = {
  id: number;
  movieId: number;
  cityId: number;
  maxPrice: number;
  /** ACTIVE, TRIGGERED, or CANCELLED. */
  status: string;
  createdAt: string;
  triggeredAt: string | null;
  triggeredShowId: number | null;
  triggeredPrice: number | null;
};

export type CreateAlertInput = { movieId: number; cityId: number; maxPrice: number };
