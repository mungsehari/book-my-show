"use client";

import { TicketIcon } from "lucide-react";
import { useState } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { BookingCard } from "@/modules/booking/components/booking-card";
import { useMyBookings } from "@/modules/booking/hooks";
import type { BookingWhen } from "@/modules/booking/types";

const TABS: { when: BookingWhen; label: string }[] = [
  { when: "upcoming", label: "Upcoming" },
  { when: "past", label: "Past" },
];

export function MyBookings() {
  const [when, setWhen] = useState<BookingWhen>("upcoming");
  const { data, isLoading, error } = useMyBookings(when);

  return (
    <div className="space-y-6">
      <div className="inline-flex gap-1 rounded-xl bg-muted p-1" role="tablist">
        {TABS.map((tab) => (
          <button
            key={tab.when}
            type="button"
            role="tab"
            aria-selected={when === tab.when}
            onClick={() => setWhen(tab.when)}
            className={cn(
              "rounded-lg px-5 py-2 text-sm font-medium transition",
              when === tab.when ? "bg-background shadow" : "text-muted-foreground",
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {isLoading && (
        <div className="space-y-3">
          <Skeleton className="h-28 w-full rounded-2xl" />
          <Skeleton className="h-28 w-full rounded-2xl" />
        </div>
      )}
      {error && <p className="text-destructive">{error.message}</p>}
      {data && data.items.length === 0 && (
        <div className="flex flex-col items-center gap-2 rounded-2xl border border-dashed py-14 text-center text-muted-foreground">
          <TicketIcon className="size-10" />
          <p>No {when} bookings yet.</p>
        </div>
      )}
      {data && data.items.length > 0 && (
        <div className="grid gap-4 sm:grid-cols-2" key={when}>
          {data.items.map((booking, index) => (
            <div
              key={booking.id}
              className="animate-fade-up"
              style={{ ["--delay" as string]: `${index * 60}ms` }}
            >
              <BookingCard booking={booking} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
