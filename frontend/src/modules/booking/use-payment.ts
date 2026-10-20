import { useRef, useState } from "react";
import { PaymentCancelled, payWithRazorpay, type RazorpaySuccess } from "@/lib/razorpay";
import { useMe } from "@/modules/auth/hooks";
import { checkoutResale } from "@/modules/booking/api";
import { useConfirmPayment, useConfirmShare, useCreateBooking } from "@/modules/booking/hooks";
import type { Checkout, PaymentProof, Share } from "@/modules/booking/types";

type Result = { bookingId: number } | null;

function toProof(paid: RazorpaySuccess): PaymentProof {
  return {
    razorpayOrderId: paid.razorpay_order_id,
    razorpayPaymentId: paid.razorpay_payment_id,
    razorpaySignature: paid.razorpay_signature,
  };
}

/** Runs the whole payment: start checkout, open Razorpay, confirm. */
export function usePayment() {
  const { data: user } = useMe();
  const createBooking = useCreateBooking();
  const confirmPayment = useConfirmPayment();
  const confirmShare = useConfirmShare();
  const [working, setWorking] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Kept so "Try again" after a closed window reuses the same booking.
  const pending = useRef<{ key: string; checkout: Checkout } | null>(null);

  async function run(
    key: string,
    description: string,
    start: () => Promise<Checkout>,
    confirm: (checkout: Checkout, proof: PaymentProof) => Promise<unknown>,
  ): Promise<Result> {
    setWorking(true);
    setError(null);
    let checkout = pending.current?.key === key ? pending.current.checkout : null;
    try {
      if (!checkout) {
        checkout = await start();
        pending.current = { key, checkout };
      }
      const paid = await payWithRazorpay({
        key: checkout.razorpayKeyId,
        orderId: checkout.razorpayOrderId,
        amountInPaise: checkout.amountInPaise,
        currency: checkout.currency,
        description,
        prefill: {
          name: user?.name,
          email: user?.email ?? undefined,
          contact: user?.phone ?? undefined,
        },
      });
      pending.current = null;
      try {
        await confirm(checkout, toProof(paid));
      } catch {
        // Money was taken. The booking page shows the real status.
      }
      return { bookingId: checkout.bookingId };
    } catch (caught) {
      if (caught instanceof PaymentCancelled) {
        setError(caught.message);
      } else {
        pending.current = null;
        setError(caught instanceof Error ? caught.message : "Payment failed.");
      }
      return null;
    } finally {
      setWorking(false);
    }
  }

  const confirmBooking = (checkout: Checkout, proof: PaymentProof) =>
    confirmPayment.mutateAsync({ id: checkout.bookingId, proof });

  /** Pay for seats held by a lock. */
  const pay = (lockId: string, description: string) =>
    run(`lock:${lockId}`, description, () => createBooking.mutateAsync(lockId), confirmBooking);

  /** Pay face value for a ticket someone else is reselling. */
  const payResale = (sellerBookingId: number, description: string) =>
    run(
      `resale:${sellerBookingId}`,
      description,
      () => checkoutResale(sellerBookingId),
      confirmBooking,
    );

  /** Pay one person's share of a split booking. */
  const payShare = (share: Share, description: string) =>
    run(
      `share:${share.id}`,
      description,
      async () => ({
        bookingId: share.bookingId,
        status: share.status,
        amount: share.amount,
        amountInPaise: share.amountInPaise,
        currency: share.currency,
        razorpayOrderId: share.razorpayOrderId,
        razorpayKeyId: share.razorpayKeyId,
      }),
      (_checkout, proof) => confirmShare.mutateAsync({ id: share.id, proof }),
    );

  return { pay, payResale, payShare, working, error };
}
