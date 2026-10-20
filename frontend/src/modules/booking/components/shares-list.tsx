"use client";

import { HandCoinsIcon } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { SharePayCard } from "@/modules/booking/components/share-pay-card";
import { useMyShares } from "@/modules/booking/hooks";

export function SharesList() {
  const { data, isLoading, error } = useMyShares();

  if (isLoading) return <Skeleton className="h-24 w-full rounded-2xl" />;
  if (error) return <p className="text-destructive">{error.message}</p>;
  if (!data || data.items.length === 0) {
    return (
      <div className="flex flex-col items-center gap-2 rounded-2xl border border-dashed py-14 text-center text-muted-foreground">
        <HandCoinsIcon className="size-10" />
        <p>Nothing to pay right now.</p>
        <p className="text-sm">When a friend splits a booking with you, it shows up here.</p>
      </div>
    );
  }

  return (
    <ul className="space-y-3">
      {data.items.map((share, index) => (
        <li
          key={share.id}
          className="animate-fade-up"
          style={{ ["--delay" as string]: `${index * 50}ms` }}
        >
          <SharePayCard share={share} />
        </li>
      ))}
    </ul>
  );
}
