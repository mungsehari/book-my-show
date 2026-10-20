export type SeatStatus = "AVAILABLE" | "LOCKED" | "BOOKED" | "UNAVAILABLE";

export type SeatMapSeat = {
  id: number;
  number: number;
  category: string;
  price: number | null;
  status: SeatStatus;
  wheelchair: boolean;
};

export type SeatMapRow = { label: string; seats: SeatMapSeat[] };

export type CategoryPrice = {
  category: string;
  price: number;
  originalPrice: number;
};

export type DealInfo = { percentOff: number; endsAt: string };

export type SeatMap = {
  showId: number;
  movieId: number | null;
  movieName: string | null;
  showDate: string;
  startTime: string;
  screenId: number;
  screenName: string;
  prices: CategoryPrice[];
  rows: SeatMapRow[];
  deal: DealInfo | null;
};

export type SeatLock = {
  lockId: string;
  showId: number;
  seatIds: number[];
  totalPrice: number;
  expiresAt: string;
};

export const MAX_SEATS = 10;
