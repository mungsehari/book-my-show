"use client";

import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { formatPrice, formatTime } from "@/lib/format";
import { useAdminBookings } from "@/modules/admin/hooks";

const VARIANT: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
  CONFIRMED: "default",
  PENDING_PAYMENT: "secondary",
  CANCELLED: "destructive",
};

export function RecentBookings() {
  const { data, isLoading, error } = useAdminBookings(null, 8);

  if (isLoading) return <Skeleton className="h-40 w-full rounded-2xl" />;
  if (error) return <p className="text-sm text-destructive">{error.message}</p>;
  if (!data || data.items.length === 0) {
    return <p className="rounded-xl bg-muted p-4 text-center text-sm text-muted-foreground">No bookings yet.</p>;
  }

  return (
    <ul className="divide-y">
      {data.items.map((booking, index) => (
        <li
          key={booking.id}
          className="animate-fade-up flex flex-wrap items-center justify-between gap-2 py-3"
          style={{ ["--delay" as string]: `${index * 50}ms` }}
        >
          <span className="text-sm">
            <span className="block font-medium">
              #{booking.id} · {booking.movieName}
            </span>
            <span className="block text-xs text-muted-foreground">
              {booking.showDate} {formatTime(booking.startTime)} · {booking.seatLabels.join(", ")}
            </span>
          </span>
          <span className="flex items-center gap-3">
            <span className="text-sm font-semibold">{formatPrice(booking.amount)}</span>
            <Badge variant={VARIANT[booking.status] ?? "outline"}>{booking.status}</Badge>
          </span>
        </li>
      ))}
    </ul>
  );
}
