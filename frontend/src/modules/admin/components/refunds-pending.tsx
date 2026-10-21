"use client";

import { CheckCircle2Icon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { formatPrice } from "@/lib/format";
import { ResultNote } from "@/modules/admin/components/result-note";
import { useAdminBookings, useAdminRole, useRefund } from "@/modules/admin/hooks";

/** Bookings that were paid after the seats were sold. Only full admins can refund. */
export function RefundsPending() {
  const { data, isLoading } = useAdminBookings("NEEDS_REFUND");
  const { isAdmin } = useAdminRole();
  const refund = useRefund();

  if (isLoading) return <Skeleton className="h-24 w-full rounded-2xl" />;
  if (!data || data.items.length === 0) {
    return (
      <p className="flex items-center justify-center gap-2 rounded-xl bg-emerald-500/10 p-4 text-sm text-emerald-600">
        <CheckCircle2Icon className="size-4" /> No refunds waiting. All clear.
      </p>
    );
  }

  return (
    <div className="space-y-3">
      <ResultNote error={refund.error?.message} success={refund.isSuccess ? "Refunded." : null} />
      <ul className="space-y-2">
        {data.items.map((booking) => (
          <li
            key={booking.id}
            className="animate-fade-up flex flex-wrap items-center justify-between gap-3 rounded-xl border p-3"
          >
            <span className="text-sm">
              <span className="block font-medium">
                #{booking.id} · {booking.movieName}
              </span>
              <span className="block text-xs text-muted-foreground">
                {booking.seatLabels.join(", ")} · {formatPrice(booking.amount)}
              </span>
            </span>
            {isAdmin ? (
              <Button
                size="sm"
                variant="destructive"
                disabled={refund.isPending}
                onClick={() => {
                  if (window.confirm(`Refund booking #${booking.id}?`)) refund.mutate(booking.id);
                }}
              >
                Refund
              </Button>
            ) : (
              <span className="text-xs text-muted-foreground">Ask an admin to refund</span>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
