import { apiGet, appGet, appPost, type ListResponse, type PageResponse } from "@/lib/api";
import type {
  Booking,
  BookingWhen,
  Checkout,
  PaymentProof,
  ResaleListing,
  Share,
  Split,
  Ticket,
} from "@/modules/booking/types";

export const fetchMyBookings = (when: BookingWhen, page = 0) =>
  appGet<PageResponse<Booking>>("/api/bookings", { when, page, size: 20 });

export const fetchBooking = (id: number) => appGet<Booking>(`/api/bookings/${id}`);

export const fetchTicket = (id: number) => appGet<Ticket>(`/api/bookings/${id}/ticket`);

export const createBooking = (lockId: string) =>
  appPost<Checkout>("/api/bookings", { lockId });

export const confirmPayment = (id: number, proof: PaymentProof) =>
  appPost<Booking>(`/api/bookings/${id}/confirm`, proof);

export const cancelBooking = (id: number) =>
  appPost<Booking>(`/api/bookings/${id}/cancel`);

export const createSplit = (lockId: string, friendIds: number[]) =>
  appPost<Split>("/api/bookings/split", { lockId, friendIds });

export const fetchMyShares = () => appGet<ListResponse<Share>>("/api/payment-shares");

export const confirmShare = (id: number, proof: PaymentProof) =>
  appPost<Share>(`/api/payment-shares/${id}/confirm`, proof);

export const giftBooking = (id: number, email: string) =>
  appPost<Booking>(`/api/bookings/${id}/gift`, { email });

export const fetchResaleListings = (showId: number) =>
  apiGet<ListResponse<ResaleListing>>(`/shows/${showId}/resale`);

export const listForResale = (id: number) =>
  appPost<Booking>(`/api/bookings/${id}/resale`);

export const unlistResale = (id: number) =>
  appPost<Booking>(`/api/bookings/${id}/resale/cancel`);

export const checkoutResale = (sellerBookingId: number) =>
  appPost<Checkout>(`/api/resale/${sellerBookingId}/checkout`);
