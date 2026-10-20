"use client";

import { BellRingIcon, HourglassIcon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { formatTime } from "@/lib/format";
import { MAX_SEATS } from "@/modules/seat/types";
import { usePayment } from "@/modules/booking/use-payment";
import {
  useJoinWaitlist,
  useLeaveWaitlist,
  useMyWaitlist,
} from "@/modules/waitlist/hooks";

type Props = { showId: number; housefull: boolean; movieName: string | null };

export function WaitlistPanel({ showId, housefull, movieName }: Props) {
  const { data: entry } = useMyWaitlist(showId);
  const join = useJoinWaitlist(showId);
  const leave = useLeaveWaitlist(showId);
  const payment = usePayment();
  const router = useRouter();
  const [seats, setSeats] = useState(2);

  const active = entry && ["WAITING", "OFFERED"].includes(entry.status) ? entry : null;
  if (!active && !housefull) return null;

  if (active?.status === "OFFERED" && active.lockId) {
    const lockId = active.lockId;
    return (
      <Card tone="good">
        <BellRingIcon className="size-8 text-emerald-600" />
        <h2 className="text-lg font-bold">Good news, seats are held for you!</h2>
        <p className="text-sm text-muted-foreground">
          {active.offeredSeatIds?.length ?? active.seatsWanted} seats until{" "}
          {active.offerExpiresAt
            ? formatTime(new Date(active.offerExpiresAt).toTimeString().slice(0, 8))
            : "soon"}
          . Pay now to keep them.
        </p>
        {payment.error && <p className="text-sm text-destructive">{payment.error}</p>}
        <Button
          disabled={payment.working}
          onClick={async () => {
            const result = await payment.pay(lockId, `${movieName ?? "Show"} (waitlist seats)`);
            if (result) router.push(`/bookings/${result.bookingId}?paid=1`);
          }}
        >
          {payment.working ? "Waiting for payment..." : "Pay and book"}
        </Button>
      </Card>
    );
  }

  if (active) {
    return (
      <Card>
        <HourglassIcon className="size-8 text-primary" />
        <h2 className="text-lg font-bold">You are on the waitlist</h2>
        <p className="text-sm text-muted-foreground">
          {active.position ? `Place ${active.position} in the queue` : "In the queue"} for{" "}
          {active.seatsWanted} {active.seatsWanted === 1 ? "seat" : "seats"}. We will hold seats
          for you if someone cancels.
        </p>
        {leave.error && <p className="text-sm text-destructive">{leave.error.message}</p>}
        <Button
          variant="outline"
          disabled={leave.isPending}
          onClick={() => leave.mutate(active.entryId)}
        >
          Leave waitlist
        </Button>
      </Card>
    );
  }

  return (
    <Card>
      <HourglassIcon className="size-8 text-primary" />
      <h2 className="text-lg font-bold">This show is housefull</h2>
      <p className="text-sm text-muted-foreground">
        Join the waitlist. If someone cancels, we hold the seats for you first.
      </p>
      <div className="flex items-center gap-3">
        <label htmlFor="waitlist-seats" className="text-sm">
          Seats
        </label>
        <div className="flex items-center rounded-lg border">
          <button
            type="button"
            className="px-3 py-1.5 text-lg"
            aria-label="Fewer seats"
            onClick={() => setSeats((value) => Math.max(1, value - 1))}
          >
            −
          </button>
          <output id="waitlist-seats" className="w-8 text-center font-semibold">
            {seats}
          </output>
          <button
            type="button"
            className="px-3 py-1.5 text-lg"
            aria-label="More seats"
            onClick={() => setSeats((value) => Math.min(MAX_SEATS, value + 1))}
          >
            +
          </button>
        </div>
      </div>
      {join.error && <p className="text-sm text-destructive">{join.error.message}</p>}
      <Button disabled={join.isPending} onClick={() => join.mutate(seats)}>
        {join.isPending ? "Joining..." : "Join waitlist"}
      </Button>
    </Card>
  );
}

function Card({
  children,
  tone,
}: {
  children: React.ReactNode;
  tone?: "good";
}) {
  return (
    <div
      className={`animate-fade-up mx-auto flex max-w-md flex-col items-center gap-3 rounded-3xl border p-6 text-center shadow-lg ${
        tone === "good" ? "border-emerald-500/50 bg-emerald-500/5" : "bg-card"
      }`}
    >
      {children}
    </div>
  );
}
