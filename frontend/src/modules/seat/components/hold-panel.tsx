"use client";

import { TimerIcon } from "lucide-react";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { formatPrice } from "@/lib/format";
import type { SeatLock } from "@/modules/seat/types";

type Props = {
  lock: SeatLock;
  seatLabels: string[];
  releasing: boolean;
  paying: boolean;
  payError: string | null;
  onPay: () => void;
  onRelease: () => void;
  onExpire: () => void;
};

function secondsLeft(expiresAt: string): number {
  return Math.max(0, Math.round((new Date(expiresAt).getTime() - Date.now()) / 1000));
}

export function HoldPanel({
  lock,
  seatLabels,
  releasing,
  paying,
  payError,
  onPay,
  onRelease,
  onExpire,
}: Props) {
  const [left, setLeft] = useState(() => secondsLeft(lock.expiresAt));

  useEffect(() => {
    const timer = setInterval(() => {
      const next = secondsLeft(lock.expiresAt);
      setLeft(next);
      if (next === 0) onExpire();
    }, 1000);
    return () => clearInterval(timer);
  }, [lock.expiresAt, onExpire]);

  const minutes = String(Math.floor(left / 60)).padStart(2, "0");
  const seconds = String(left % 60).padStart(2, "0");

  return (
    <div className="animate-fade-up mx-auto max-w-md space-y-5 rounded-3xl border bg-card p-6 text-center shadow-xl">
      <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-primary/10 text-primary">
        <TimerIcon className="size-7" />
      </div>
      <div>
        <h2 className="text-xl font-bold">Your seats are on hold</h2>
        <p className="text-sm text-muted-foreground">
          Finish payment before the timer ends, or the seats go back on sale.
        </p>
      </div>
      <p
        className={`text-4xl font-extrabold tabular-nums ${left < 60 ? "text-destructive" : ""}`}
        aria-live="polite"
      >
        {minutes}:{seconds}
      </p>
      <div className="rounded-xl bg-muted p-3 text-sm">
        <p className="font-medium">{seatLabels.join(", ")}</p>
        <p className="text-muted-foreground">Total {formatPrice(lock.totalPrice)}</p>
      </div>
      {payError && (
        <p className="animate-pop-in rounded-lg bg-destructive/10 p-2 text-sm text-destructive">
          {payError}
        </p>
      )}
      <Button className="w-full" disabled={paying || releasing} onClick={onPay}>
        {paying ? "Waiting for payment..." : `Pay ${formatPrice(lock.totalPrice)}`}
      </Button>
      <Button
        variant="ghost"
        className="w-full"
        disabled={releasing || paying}
        onClick={onRelease}
      >
        {releasing ? "Releasing..." : "Release seats"}
      </Button>
    </div>
  );
}
