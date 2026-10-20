"use client";

import { GiftIcon, RepeatIcon } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  useGift,
  useListForResale,
  useResaleListings,
  useUnlistResale,
} from "@/modules/booking/hooks";
import type { Booking } from "@/modules/booking/types";

/** Gift and resale buttons for a confirmed ticket. */
export function BookingActions({ booking }: { booking: Booking }) {
  const listings = useResaleListings(booking.showId);
  const listed = listings.data?.items.some((item) => item.bookingId === booking.id) ?? false;

  return (
    <div className="mx-auto w-full max-w-sm space-y-4 text-left">
      <GiftForm bookingId={booking.id} />
      <ResaleToggle bookingId={booking.id} listed={listed} />
    </div>
  );
}

function GiftForm({ bookingId }: { bookingId: number }) {
  const gift = useGift();
  const [open, setOpen] = useState(false);
  const [email, setEmail] = useState("");

  if (gift.isSuccess) {
    return (
      <p className="animate-pop-in rounded-xl bg-emerald-500/10 p-3 text-center text-sm text-emerald-600">
        Gift sent! The ticket is now in their account.
      </p>
    );
  }

  if (!open) {
    return (
      <Button variant="outline" className="w-full" onClick={() => setOpen(true)}>
        <GiftIcon /> Gift this ticket
      </Button>
    );
  }

  return (
    <form
      className="animate-fade-up space-y-2 rounded-2xl border p-4"
      onSubmit={(event) => {
        event.preventDefault();
        if (window.confirm(`Give this ticket to ${email}? You cannot undo this.`)) {
          gift.mutate({ id: bookingId, email: email.trim() });
        }
      }}
    >
      <label htmlFor="gift-email" className="text-sm font-medium">
        Their account email
      </label>
      <input
        id="gift-email"
        type="email"
        required
        value={email}
        onChange={(event) => setEmail(event.target.value)}
        placeholder="friend@example.com"
        className="h-10 w-full rounded-lg border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
      />
      {gift.error && <p className="text-sm text-destructive">{gift.error.message}</p>}
      <div className="flex gap-2">
        <Button type="submit" size="sm" disabled={gift.isPending || !email}>
          {gift.isPending ? "Sending..." : "Send gift"}
        </Button>
        <Button type="button" size="sm" variant="ghost" onClick={() => setOpen(false)}>
          Not now
        </Button>
      </div>
    </form>
  );
}

function ResaleToggle({ bookingId, listed }: { bookingId: number; listed: boolean }) {
  const list = useListForResale();
  const unlist = useUnlistResale();
  const busy = list.isPending || unlist.isPending;
  const error = list.error ?? unlist.error;

  return (
    <div className="space-y-2">
      {listed ? (
        <>
          <p className="rounded-xl bg-amber-500/10 p-3 text-center text-sm text-amber-700 dark:text-amber-300">
            Your ticket is listed for resale at the price you paid.
          </p>
          <Button
            variant="outline"
            className="w-full"
            disabled={busy}
            onClick={() => unlist.mutate(bookingId)}
          >
            Stop resale
          </Button>
        </>
      ) : (
        <Button
          variant="outline"
          className="w-full"
          disabled={busy}
          onClick={() => list.mutate(bookingId)}
        >
          <RepeatIcon /> {busy ? "Listing..." : "Resell at face value"}
        </Button>
      )}
      {error && <p className="text-sm text-destructive">{error.message}</p>}
    </div>
  );
}
