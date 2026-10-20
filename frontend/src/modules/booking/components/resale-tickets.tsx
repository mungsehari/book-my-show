"use client";

import { RepeatIcon } from "lucide-react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { formatPrice } from "@/lib/format";
import { useResaleListings } from "@/modules/booking/hooks";
import { usePayment } from "@/modules/booking/use-payment";

/** Tickets other people sell at face value. Hidden when there are none. */
export function ResaleTickets({ showId, movieName }: { showId: number; movieName: string | null }) {
  const { data } = useResaleListings(showId);
  const payment = usePayment();
  const router = useRouter();

  if (!data || data.items.length === 0) return null;

  return (
    <section className="animate-fade-up space-y-3 rounded-2xl border bg-card p-4">
      <h2 className="flex items-center gap-2 font-semibold">
        <RepeatIcon className="size-5 text-primary" /> Resale tickets
      </h2>
      <p className="text-xs text-muted-foreground">
        Sold at the price the owner paid, never more.
      </p>
      {payment.error && <p className="text-sm text-destructive">{payment.error}</p>}
      <ul className="space-y-2">
        {data.items.map((listing) => (
          <li
            key={listing.bookingId}
            className="flex flex-wrap items-center justify-between gap-3 rounded-xl border p-3"
          >
            <span>
              <span className="block font-medium">{listing.seats.join(", ")}</span>
              <span className="block text-sm text-muted-foreground">
                {formatPrice(listing.price)}
                {listing.onHold && " · someone is paying"}
              </span>
            </span>
            <Button
              size="sm"
              disabled={payment.working || listing.onHold}
              onClick={async () => {
                const result = await payment.payResale(
                  listing.bookingId,
                  `${movieName ?? "Show"} resale (${listing.seats.join(", ")})`,
                );
                if (result) router.push(`/bookings/${result.bookingId}?paid=1`);
              }}
            >
              {payment.working ? "Paying..." : "Buy"}
            </Button>
          </li>
        ))}
      </ul>
    </section>
  );
}
