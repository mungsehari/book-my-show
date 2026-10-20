import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  cancelBooking,
  confirmPayment,
  confirmShare,
  createBooking,
  createSplit,
  fetchBooking,
  fetchMyShares,
  fetchMyBookings,
  fetchResaleListings,
  fetchTicket,
  giftBooking,
  listForResale,
  unlistResale,
} from "@/modules/booking/api";
import type { BookingWhen, PaymentProof } from "@/modules/booking/types";

export const useMyBookings = (when: BookingWhen) =>
  useQuery({
    queryKey: ["bookings", "me", when],
    queryFn: () => fetchMyBookings(when),
  });

export const useBooking = (id: number) =>
  useQuery({
    queryKey: ["bookings", id],
    queryFn: () => fetchBooking(id),
    // A PENDING booking is waiting for the payment webhook, so check again soon.
    refetchInterval: (query) => (query.state.data?.status === "PENDING_PAYMENT" ? 3_000 : false),
  });

export const useTicket = (id: number, enabled: boolean) =>
  useQuery({
    queryKey: ["bookings", id, "ticket"],
    queryFn: () => fetchTicket(id),
    enabled,
  });

export const useCreateBooking = () => useMutation({ mutationFn: createBooking });

export function useConfirmPayment() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; proof: PaymentProof }) =>
      confirmPayment(input.id, input.proof),
    onSettled: () => client.invalidateQueries({ queryKey: ["bookings"] }),
  });
}

export function useCancelBooking() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: cancelBooking,
    onSettled: () => client.invalidateQueries({ queryKey: ["bookings"] }),
  });
}

/** Your unpaid shares of split bookings, checked often so new requests appear. */
export const useMyShares = (enabled = true) =>
  useQuery({
    queryKey: ["shares", "me"],
    queryFn: fetchMyShares,
    enabled,
    refetchInterval: 8_000,
  });

export const useCreateSplit = () => {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (input: { lockId: string; friendIds: number[] }) =>
      createSplit(input.lockId, input.friendIds),
    onSuccess: () => client.invalidateQueries({ queryKey: ["shares"] }),
  });
};

export function useConfirmShare() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; proof: PaymentProof }) =>
      confirmShare(input.id, input.proof),
    onSettled: () => {
      client.invalidateQueries({ queryKey: ["shares"] });
      client.invalidateQueries({ queryKey: ["bookings"] });
    },
  });
}

/** Tickets offered for resale at one show. Public data. */
export const useResaleListings = (showId: number, enabled = true) =>
  useQuery({
    queryKey: ["resale", showId],
    queryFn: () => fetchResaleListings(showId),
    enabled,
    refetchInterval: 10_000,
  });

/** Gift or resale change a booking, so refresh every booking and resale list. */
function useBookingChange<TInput>(call: (input: TInput) => Promise<unknown>) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: call,
    onSettled: () => {
      client.invalidateQueries({ queryKey: ["bookings"] });
      client.invalidateQueries({ queryKey: ["resale"] });
    },
  });
}

export const useGift = () =>
  useBookingChange((input: { id: number; email: string }) => giftBooking(input.id, input.email));
export const useListForResale = () => useBookingChange(listForResale);
export const useUnlistResale = () => useBookingChange(unlistResale);
