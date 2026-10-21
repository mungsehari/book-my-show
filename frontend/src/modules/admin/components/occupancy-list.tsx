"use client";

import { useState } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { formatTime, todayIso } from "@/lib/format";
import { cn } from "@/lib/utils";
import { useOccupancy } from "@/modules/admin/hooks";
import type { CrowdLevel } from "@/modules/admin/types";

const LEVEL_STYLE: Record<CrowdLevel, { bar: string; label: string }> = {
  LOW: { bar: "bg-emerald-500", label: "Plenty of seats" },
  MEDIUM: { bar: "bg-amber-400", label: "Filling up" },
  HIGH: { bar: "bg-orange-500", label: "Almost full" },
  HOUSEFULL: { bar: "bg-rose-600", label: "Housefull" },
};

export function OccupancyList() {
  const [date, setDate] = useState(() => todayIso());
  const { data, isLoading, error } = useOccupancy(date);

  return (
    <div className="space-y-4">
      <label className="inline-flex items-center gap-2 text-sm">
        <span className="text-muted-foreground">Date</span>
        <input
          type="date"
          value={date}
          onChange={(event) => event.target.value && setDate(event.target.value)}
          className="h-9 rounded-lg border bg-background px-2"
        />
      </label>

      {isLoading && <Skeleton className="h-40 w-full rounded-2xl" />}
      {error && <p className="text-sm text-destructive">{error.message}</p>}
      {data && data.items.length === 0 && (
        <p className="rounded-xl bg-muted p-4 text-center text-sm text-muted-foreground">No shows on this date.</p>
      )}
      <ul className="space-y-3">
        {data?.items.map((show, index) => {
          const style = LEVEL_STYLE[show.level] ?? LEVEL_STYLE.LOW;
          return (
            <li key={show.showId} className="space-y-1.5">
              <div className="flex items-baseline justify-between gap-3 text-sm">
                <span className="truncate font-medium">
                  {formatTime(show.startTime)} · {show.movieName}
                </span>
                <span className="shrink-0 text-muted-foreground">
                  {show.sold + show.held} of {show.totalSeats} · {style.label}
                </span>
              </div>
              <div className="h-2.5 overflow-hidden rounded-full bg-muted">
                <div
                  className={cn("animate-grow-x h-full rounded-full", style.bar)}
                  style={{ width: `${Math.min(show.percentFull, 100)}%`, ["--delay" as string]: `${index * 60}ms` }}
                />
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
