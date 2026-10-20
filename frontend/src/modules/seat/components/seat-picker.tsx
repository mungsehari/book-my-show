"use client";

import { useRouter } from "next/navigation";
import { useCallback, useState } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { formatTime } from "@/lib/format";
import { SplitSection } from "@/modules/booking/components/split-section";
import { ResaleTickets } from "@/modules/booking/components/resale-tickets";
import { usePayment } from "@/modules/booking/use-payment";
import { WaitlistPanel } from "@/modules/waitlist/components/waitlist-panel";
import { HoldPanel } from "@/modules/seat/components/hold-panel";
import { SeatGrid } from "@/modules/seat/components/seat-grid";
import { SeatLegend } from "@/modules/seat/components/seat-legend";
import { SummaryBar } from "@/modules/seat/components/summary-bar";
import { useLockSeats, useReleaseLock, useSeatMap } from "@/modules/seat/hooks";
import type { SeatLock } from "@/modules/seat/types";
import { useSeatSelection } from "@/modules/seat/use-seat-selection";

export function SeatPicker({ showId }: { showId: number }) {
  const seatMap = useSeatMap(showId);
  const selection = useSeatSelection(seatMap.data);
  const lockMutation = useLockSeats(showId);
  const releaseMutation = useReleaseLock(showId);
  const [lock, setLock] = useState<SeatLock | null>(null);
  const [heldLabels, setHeldLabels] = useState<string[]>([]);
  const [expired, setExpired] = useState(false);
  const payment = usePayment();
  const router = useRouter();

  const { clear } = selection;
  const handleExpire = useCallback(() => {
    setLock(null);
    clear();
    setExpired(true);
  }, [clear]);

  if (seatMap.isLoading) return <Skeleton className="h-96 w-full rounded-3xl" />;
  if (seatMap.error || !seatMap.data) {
    return (
      <p className="text-destructive">
        {seatMap.error?.message ?? "Could not load the seat map."}
      </p>
    );
  }
  const data = seatMap.data;

  function proceed() {
    setExpired(false);
    lockMutation.mutate(selection.selectedIds, {
      onSuccess: (created) => {
        setHeldLabels(selection.selected.map((seat) => seat.label));
        setLock(created);
      },
    });
  }

  async function pay() {
    if (!lock) return;
    const result = await payment.pay(lock.lockId, `${data.movieName} · ${heldLabels.join(", ")}`);
    if (result) router.push(`/bookings/${result.bookingId}?paid=1`);
  }

  function release() {
    if (!lock) return;
    releaseMutation.mutate(lock.lockId, {
      onSuccess: () => {
        setLock(null);
        selection.clear();
      },
    });
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-extrabold tracking-tight">{data.movieName}</h1>
        <p className="text-sm text-muted-foreground">
          {data.screenName} ·{" "}
          {new Date(`${data.showDate}T00:00:00`).toLocaleDateString("en-IN", {
            weekday: "short",
            day: "numeric",
            month: "short",
          })}{" "}
          · {formatTime(data.startTime)}
        </p>
        {data.deal && (
          <p className="mt-2 inline-block rounded-full bg-emerald-500/15 px-3 py-1 text-xs font-medium text-emerald-600 dark:text-emerald-400">
            Last-minute deal: {data.deal.percentOff}% off
          </p>
        )}
      </header>

      {!lock && (
        <WaitlistPanel
          showId={showId}
          movieName={data.movieName}
          housefull={data.rows.every((row) =>
            row.seats.every((seat) => seat.status !== "AVAILABLE"),
          )}
        />
      )}

      {!lock && <ResaleTickets showId={showId} movieName={data.movieName} />}

      {expired && (
        <p className="animate-pop-in rounded-xl bg-destructive/10 p-3 text-sm text-destructive">
          Your hold ran out and the seats were released. Pick your seats again.
        </p>
      )}

      {lock ? (
        <div className="space-y-4">
          <HoldPanel
            lock={lock}
            seatLabels={heldLabels}
            releasing={releaseMutation.isPending}
            paying={payment.working}
            payError={payment.error}
            onPay={pay}
            onRelease={release}
            onExpire={handleExpire}
          />
          <SplitSection lockId={lock.lockId} total={lock.totalPrice} />
        </div>
      ) : (
        <>
          <div className="space-y-2 text-center">
            <div className="mx-auto h-2 w-3/4 max-w-md rounded-full bg-gradient-to-r from-transparent via-primary/60 to-transparent shadow-[0_8px_24px_-4px] shadow-primary/40" />
            <p className="text-xs uppercase tracking-widest text-muted-foreground">
              Screen this way
            </p>
          </div>
          <SeatGrid
            seatMap={data}
            selectedIds={selection.selectedIds}
            onToggle={selection.toggle}
          />
          <SeatLegend />
          <SummaryBar
            labels={selection.selected.map((seat) => seat.label)}
            total={selection.total}
            percentOff={data.deal?.percentOff}
            notice={
              selection.lostCount > 0
                ? "Someone just took a seat you picked."
                : selection.notice
            }
            error={lockMutation.error?.message ?? null}
            locking={lockMutation.isPending}
            onProceed={proceed}
            onClear={selection.clear}
          />
        </>
      )}
    </div>
  );
}
