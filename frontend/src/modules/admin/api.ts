import { apiGet, appGet, appPost, type ListResponse, type PageResponse } from "@/lib/api";
import type { Booking } from "@/modules/booking/types";
import type {
  CheckedInTicket,
  DailyStats,
  MovieInput,
  Occupancy,
  PlaceInput,
  Summary,
  TopMovies,
  ScreenInput,
  ScreenLayout,
  ScreenSummary,
  ShowInput,
  ViewInput,
} from "@/modules/admin/types";

export const fetchSummary = (days: number) =>
  appGet<Summary>("/api/admin/stats/summary", { days });

export const fetchDaily = (days: number) =>
  appGet<DailyStats>("/api/admin/stats/daily", { days });

export const fetchTopMovies = (days: number) =>
  appGet<TopMovies>("/api/admin/stats/top-movies", { days, limit: 5 });

export const fetchOccupancy = (date: string) =>
  appGet<Occupancy>("/api/admin/stats/occupancy", { date });

export const fetchAdminBookings = (status: string | null, size: number) =>
  appGet<PageResponse<Booking>>("/api/admin/bookings", { status, size });

export const fetchScreens = (cinemaId: number) =>
  apiGet<ListResponse<ScreenSummary>>(`/cinemas/${cinemaId}/screens`);

export const fetchLayout = (screenId: number) =>
  apiGet<ScreenLayout>(`/screens/${screenId}/layout`);

export const addMovie = (input: MovieInput) => appPost<{ id: number }>("/api/admin/movies", input);

export const addScreen = (cinemaId: number, input: ScreenInput) =>
  appPost<ScreenSummary>(`/api/admin/cinemas/${cinemaId}/screens`, input);

export const addShow = (screenId: number, input: ShowInput) =>
  appPost<{ id: number }>(`/api/admin/screens/${screenId}/shows`, input);

export const addPlace = (cinemaId: number, input: PlaceInput) =>
  appPost<unknown>(`/api/admin/cinemas/${cinemaId}/places`, input);

export const addView = (screenId: number, input: ViewInput) =>
  appPost<unknown>(`/api/admin/screens/${screenId}/views`, input);

export const checkIn = (ticketCode: string) =>
  appPost<CheckedInTicket>("/api/admin/tickets/check-in", { ticketCode });

export const assignTheatreAdmin = (cinemaId: number, userId: number) =>
  appPost<unknown>(`/api/admin/cinemas/${cinemaId}/admins`, { userId });

export const refundBooking = (bookingId: number) =>
  appPost<{ id: number; status: string }>(`/api/admin/bookings/${bookingId}/refund`);
