import { CalendarIcon, ClockIcon, ArmchairIcon } from "lucide-react";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { formatPrice, formatTime } from "@/lib/format";
import type { Booking } from "@/modules/booking/types";

const STATUS_VARIANT: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
  CONFIRMED: "default",
  PENDING_PAYMENT: "secondary",
  CANCELLED: "destructive",
  RESOLD: "outline",
  REFUNDED: "outline",
};

export function BookingCard({ booking }: { booking: Booking }) {
  return (
    <Link
      href={`/bookings/${booking.id}`}
      className="block rounded-2xl border bg-card p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="font-semibold">{booking.movieName ?? "Movie"}</h3>
          <p className="text-xs text-muted-foreground">{booking.screenName}</p>
        </div>
        <Badge variant={STATUS_VARIANT[booking.status] ?? "outline"}>
          {booking.status}
        </Badge>
      </div>
      <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-sm text-muted-foreground">
        <span className="flex items-center gap-1">
          <CalendarIcon className="size-4" />
          {new Date(`${booking.showDate}T00:00:00`).toLocaleDateString("en-IN", {
            day: "numeric",
            month: "short",
            year: "numeric",
          })}
        </span>
        <span className="flex items-center gap-1">
          <ClockIcon className="size-4" />
          {formatTime(booking.startTime)}
        </span>
        <span className="flex items-center gap-1">
          <ArmchairIcon className="size-4" />
          {booking.seatLabels.join(", ")}
        </span>
      </div>
      <p className="mt-3 text-right font-semibold">{formatPrice(booking.amount)}</p>
    </Link>
  );
}
