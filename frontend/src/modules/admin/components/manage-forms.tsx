"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { CinemaSelect } from "@/modules/admin/components/cinema-select";
import { ResultNote } from "@/modules/admin/components/result-note";
import { useAssignAdmin, useRefund } from "@/modules/admin/hooks";

const INPUT =
  "h-10 w-full rounded-lg border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring";

export function AssignAdminForm() {
  const [cinemaId, setCinemaId] = useState<number | null>(null);
  const [userId, setUserId] = useState("");
  const mutation = useAssignAdmin();

  return (
    <section className="animate-fade-up space-y-4 rounded-3xl border bg-card p-5">
      <div>
        <h2 className="font-semibold">Make a theatre admin</h2>
        <p className="text-sm text-muted-foreground">
          The user can then add screens and shows for this cinema.
        </p>
      </div>
      <CinemaSelect idPrefix="assign" onChange={setCinemaId} />
      <label className="block space-y-1.5 text-sm font-medium">
        User ID
        <input type="number" min={1} value={userId} onChange={(e) => setUserId(e.target.value)} className={INPUT} />
      </label>
      <ResultNote success={mutation.isSuccess ? "Done. They are a theatre admin now." : null} error={mutation.error?.message} />
      <Button
        disabled={cinemaId === null || !userId || mutation.isPending}
        onClick={() => cinemaId !== null && mutation.mutate({ cinemaId, userId: Number(userId) })}
      >
        {mutation.isPending ? "Saving..." : "Assign"}
      </Button>
    </section>
  );
}

export function RefundForm() {
  const [bookingId, setBookingId] = useState("");
  const mutation = useRefund();

  return (
    <section className="animate-fade-up space-y-4 rounded-3xl border bg-card p-5">
      <div>
        <h2 className="font-semibold">Refund a booking</h2>
        <p className="text-sm text-muted-foreground">
          Only for bookings that were paid after the seats were sold (needs refund).
        </p>
      </div>
      <label className="block space-y-1.5 text-sm font-medium">
        Booking ID
        <input type="number" min={1} value={bookingId} onChange={(e) => setBookingId(e.target.value)} className={INPUT} />
      </label>
      <ResultNote
        success={mutation.isSuccess ? `Refunded. Booking is now ${mutation.data.status}.` : null}
        error={mutation.error?.message}
      />
      <Button
        variant="destructive"
        disabled={!bookingId || mutation.isPending}
        onClick={() => {
          if (window.confirm(`Refund booking #${bookingId}?`)) mutation.mutate(Number(bookingId));
        }}
      >
        {mutation.isPending ? "Refunding..." : "Refund"}
      </Button>
    </section>
  );
}
