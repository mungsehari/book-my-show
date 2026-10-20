"use client";

import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { formatPrice } from "@/lib/format";
import { usePayment } from "@/modules/booking/use-payment";
import type { Share } from "@/modules/booking/types";

type Props = {
  share: Share;
  /** Where to go after paying. The booking page by default. */
  redirect?: boolean;
};

/** One share with a Pay button. */
export function SharePayCard({ share, redirect = true }: Props) {
  const payment = usePayment();
  const router = useRouter();

  return (
    <div className="space-y-2 rounded-2xl border bg-card p-4 text-left">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="font-semibold">Your share: {formatPrice(share.amount)}</p>
          <p className="text-xs text-muted-foreground">Booking #{share.bookingId}</p>
        </div>
        <Button
          disabled={payment.working}
          onClick={async () => {
            const result = await payment.payShare(share, `Share of booking #${share.bookingId}`);
            if (result && redirect) router.push(`/bookings/${result.bookingId}`);
          }}
        >
          {payment.working ? "Waiting for payment..." : `Pay ${formatPrice(share.amount)}`}
        </Button>
      </div>
      {payment.error && <p className="text-sm text-destructive">{payment.error}</p>}
    </div>
  );
}
