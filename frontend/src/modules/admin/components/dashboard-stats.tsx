"use client";

import { IndianRupeeIcon, ReceiptIcon, TicketIcon, TrendingUpIcon } from "lucide-react";
import { formatPrice } from "@/lib/format";
import { StatCard } from "@/modules/admin/components/stat-card";
import { useSummary } from "@/modules/admin/hooks";

export function DashboardStats({ days }: { days: number }) {
  const { data, error } = useSummary(days);

  return (
    <div className="space-y-2">
      {error && <p className="text-sm text-destructive">{error.message}</p>}
      <div className="grid grid-cols-2 gap-4 xl:grid-cols-4">
        <StatCard
          label="Revenue"
          icon={IndianRupeeIcon}
          tint="from-rose-500 to-orange-400"
          value={data?.revenue}
          format={(n) => formatPrice(n)}
          note={data ? `${data.cancelled} cancelled` : undefined}
          delay={0}
        />
        <StatCard
          label="Tickets sold"
          icon={TicketIcon}
          tint="from-violet-500 to-fuchsia-400"
          value={data?.ticketsSold}
          delay={70}
        />
        <StatCard
          label="Bookings"
          icon={ReceiptIcon}
          tint="from-sky-500 to-cyan-400"
          value={data?.bookings}
          note={data ? `${data.refundsPending} refunds pending` : undefined}
          delay={140}
        />
        <StatCard
          label="Average ticket"
          icon={TrendingUpIcon}
          tint="from-emerald-500 to-lime-400"
          value={data?.averageTicketPrice}
          format={(n) => formatPrice(n)}
          delay={210}
        />
      </div>
    </div>
  );
}
