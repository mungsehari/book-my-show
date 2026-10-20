"use client";

import { QRCodeSVG } from "qrcode.react";
import { formatPrice, formatTime } from "@/lib/format";
import { Skeleton } from "@/components/ui/skeleton";
import { useTicket } from "@/modules/booking/hooks";

export function TicketCard({ bookingId }: { bookingId: number }) {
  const { data, isLoading, error } = useTicket(bookingId, true);

  if (isLoading) return <Skeleton className="h-72 w-full rounded-3xl" />;
  if (error || !data) {
    return <p className="text-sm text-destructive">{error?.message ?? "Ticket not ready."}</p>;
  }

  return (
    <div className="animate-fade-up mx-auto max-w-sm overflow-hidden rounded-3xl border bg-card shadow-xl">
      <div className="bg-gradient-to-br from-rose-600 to-orange-500 p-5 text-white">
        <h3 className="text-lg font-extrabold">{data.movieName}</h3>
        <p className="text-sm text-white/85">{data.cinemaName}</p>
      </div>
      <div className="space-y-3 p-5 text-sm">
        <div className="grid grid-cols-2 gap-3">
          <Info label="Date" value={new Date(`${data.showDate}T00:00:00`).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })} />
          <Info label="Time" value={formatTime(data.startTime)} />
          <Info label="Screen" value={data.screenName} />
          <Info label="Seats" value={data.seats.join(", ")} />
        </div>
        <div className="flex justify-center rounded-2xl bg-white p-4">
          <QRCodeSVG value={data.ticketCode} size={176} />
        </div>
        <p className="text-center text-xs text-muted-foreground">
          Show this QR at the entry. It works only once.
        </p>
        <p className="text-center font-semibold">{formatPrice(data.amount)}</p>
        {data.checkedInAt && (
          <p className="text-center text-xs text-amber-600">Already used for entry.</p>
        )}
      </div>
    </div>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs uppercase tracking-wide text-muted-foreground">{label}</p>
      <p className="font-medium">{value}</p>
    </div>
  );
}
