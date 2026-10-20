export type TimeSlot = "MORNING" | "AFTERNOON" | "EVENING";

export type PlanRequest = {
  cityId: number;
  date: string;
  mood?: string;
  people: number;
  budgetPerPerson?: number;
  time: TimeSlot;
};

export type PlaceSummary = {
  name: string;
  walkMinutes: number | null;
  notes: string | null;
};

export type PlanOption = {
  showId: number;
  movieId: number;
  movieName: string;
  cinemaId: number;
  cinemaName: string;
  startTime: string;
  seatsAvailable: number;
  pricePerPerson: number;
  totalPrice: number;
  food: PlaceSummary | null;
  parking: PlaceSummary | null;
};

export type PlanResult = {
  date: string;
  /** claude, cache, keyword, or none: how the mood was read. */
  source: string;
  plans: PlanOption[];
};
