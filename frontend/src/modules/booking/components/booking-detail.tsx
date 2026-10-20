"use client";

import { CheckIcon, Loader2Icon } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { BookingActions } from "@/modules/booking/components/booking-actions";
import { Confetti } from "@/modules/booking/components/confetti";
import { SharePayCard } from "@/modules/booking/components/share-pay-card";
import { TicketCard } from "@/modules/booking/components/ticket-card";
import { useBooking, useCancelBooking, useMyShares } from "@/modules/booking/hooks";

type Props = { bookingId: number; justPaid: boolean };

export function BookingDetail({ bookingId, justPaid }: Props) {
  const { data, isLoading, error } = useBooking(bookingId);
  const cancel = useCancelBooking();
  const shares = useMyShares(data?.status === "PENDING_PAYMENT");
  const myShare = shares.data?.items.find((share) => share.bookingId === bookingId);

  if (isLoading) return <Skeleton className="h-80 w-full rounded-3xl" />;
  if (error || !data) {
    return <p className="text-destructive">{error?.message ?? "Booking not found."}</p>;
  }

  const confirmed = data.status === "CONFIRMED";
  const pending = data.status === "PENDING_PAYMENT";

  return (
    <div className="relative space-y-6 text-center">
      {confirmed && justPaid && <Confetti />}

      {confirmed && (
        <header className="space-y-2">
          <div className="animate-pop-in mx-auto flex size-16 items-center justify-center rounded-full bg-emerald-500 text-white shadow-lg">
            <CheckIcon className="size-9" />
          </div>
          <h1 className="text-2xl font-extrabold">
            {justPaid ? "Booking confirmed!" : "Your booking"}
          </h1>
          <p className="text-sm text-muted-foreground">Booking #{data.id}</p>
        </header>
      )}

      {pending && (
        <div className="mx-auto flex max-w-md flex-col items-center gap-3 rounded-2xl border p-6">
          <Loader2Icon className="size-8 animate-spin text-primary" />
          {myShare ? (
            <>
              <p className="font-medium">Waiting for everyone to pay</p>
              <p className="text-sm text-muted-foreground">
                Tickets are issued when all shares are paid. Your share is still open.
              </p>
              <SharePayCard share={myShare} redirect={false} />
            </>
          ) : (
            <>
              <p className="font-medium">Confirming your payment...</p>
              <p className="text-sm text-muted-foreground">
                If you paid, or your friends are still paying, this updates on its own.
              </p>
            </>
          )}
        </div>
      )}

      {!confirmed && !pending && (
        <div className="mx-auto max-w-md rounded-2xl border p-6">
          <h1 className="text-xl font-bold">Booking {data.status.toLowerCase()}</h1>
          <p className="text-sm text-muted-foreground">Booking #{data.id}</p>
        </div>
      )}

      {confirmed && <TicketCard bookingId={data.id} />}
      {confirmed && <BookingActions booking={data} />}

      <div className="flex flex-col items-center gap-3">
        {cancel.error && <p className="text-sm text-destructive">{cancel.error.message}</p>}
        {confirmed && (
          <Button
            variant="outline"
            disabled={cancel.isPending}
            onClick={() => {
              if (window.confirm("Cancel this booking?")) cancel.mutate(data.id);
            }}
          >
            {cancel.isPending ? "Cancelling..." : "Cancel booking"}
          </Button>
        )}
        <Button asChild variant="ghost">
          <Link href="/bookings">All my bookings</Link>
        </Button>
      </div>
    </div>
  );
}
