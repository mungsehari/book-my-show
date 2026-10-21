export type SeatCategory = "REGULAR" | "PREMIUM" | "RECLINER";

export const CATEGORIES: SeatCategory[] = ["REGULAR", "PREMIUM", "RECLINER"];

export const CATEGORY_LABEL: Record<SeatCategory, string> = {
  REGULAR: "Regular",
  PREMIUM: "Premium",
  RECLINER: "Recliner",
};

/** Colors used by the screen builder preview. */
export const CATEGORY_STYLE: Record<SeatCategory, string> = {
  REGULAR: "bg-sky-500/80 text-white",
  PREMIUM: "bg-violet-500/80 text-white",
  RECLINER: "bg-amber-500/80 text-white",
};

export type MovieInput = {
  title: string;
  description: string;
  language: string;
  genre: string;
  durationMinutes: number;
  certificate: "U" | "UA" | "A";
  releaseDate: string;
  posterUrl?: string;
};

export type RowInput = {
  label: string;
  seatCount: number;
  category: SeatCategory;
  wheelchairSeats?: number[];
};

export type ScreenInput = { name: string; rows: RowInput[] };

export type ShowInput = {
  movieId: number;
  showDate: string;
  startTime: string;
  prices: Partial<Record<SeatCategory, number>>;
  quiet: boolean;
  sensoryFriendly: boolean;
  subtitles: boolean;
  lastMinuteDeals: boolean;
};

export type PlaceInput = {
  type: "FOOD" | "PARKING";
  name: string;
  walkMinutes: number;
  notes?: string;
};

export type ViewInput = {
  fromRow: string;
  toRow: string;
  imageUrl: string;
  caption?: string;
};

export type ScreenSummary = { id: number; cinemaId: number; name: string; seatCount: number };

export type ScreenLayout = {
  screenId: number;
  rows: { label: string; seats: { id: number; number: number; category: SeatCategory; wheelchair: boolean }[] }[];
};

export type CheckedInTicket = {
  bookingId: number;
  movieName: string | null;
  cinemaName: string;
  screenName: string;
  showDate: string;
  startTime: string;
  seats: string[];
};

export type Summary = {
  days: number;
  bookings: number;
  ticketsSold: number;
  revenue: number;
  averageTicketPrice: number;
  cancelled: number;
  refundsPending: number;
};

export type DailyPoint = { date: string; bookings: number; tickets: number; revenue: number };

export type DailyStats = { days: number; points: DailyPoint[] };

export type TopMovie = { movieName: string; tickets: number; revenue: number };

export type TopMovies = { days: number; items: TopMovie[] };

export type CrowdLevel = "LOW" | "MEDIUM" | "HIGH" | "HOUSEFULL";

export type ShowOccupancy = {
  showId: number;
  cinemaId: number;
  movieName: string | null;
  showDate: string;
  startTime: string;
  totalSeats: number;
  sold: number;
  held: number;
  percentFull: number;
  level: CrowdLevel;
};

export type Occupancy = { date: string; items: ShowOccupancy[] };

export const RANGES = [7, 30, 90] as const;
export type Range = (typeof RANGES)[number];

export const ADMIN_ROLES = ["ADMIN", "THEATRE_ADMIN"];
