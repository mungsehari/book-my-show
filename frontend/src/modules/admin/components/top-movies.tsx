"use client";

import { Skeleton } from "@/components/ui/skeleton";
import { formatPrice } from "@/lib/format";
import { useTopMovies } from "@/modules/admin/hooks";

const BAR_COLORS = [
  "from-rose-500 to-orange-400",
  "from-violet-500 to-fuchsia-400",
  "from-sky-500 to-cyan-400",
  "from-emerald-500 to-lime-400",
  "from-amber-500 to-red-400",
];

export function TopMovies({ days }: { days: number }) {
  const { data, isLoading, error } = useTopMovies(days);

  if (isLoading) return <Skeleton className="h-48 w-full rounded-2xl" />;
  if (error || !data) return <p className="text-sm text-destructive">{error?.message}</p>;
  if (data.items.length === 0) {
    return <p className="rounded-xl bg-muted p-4 text-center text-sm text-muted-foreground">No sales yet.</p>;
  }

  const max = Math.max(...data.items.map((item) => item.revenue), 1);
  return (
    <ol className="space-y-4">
      {data.items.map((item, index) => (
        <li key={item.movieName} className="space-y-1.5">
          <div className="flex items-baseline justify-between gap-3 text-sm">
            <span className="truncate font-medium">
              {index + 1}. {item.movieName}
            </span>
            <span className="shrink-0 text-muted-foreground">
              {item.tickets} {item.tickets === 1 ? "ticket" : "tickets"} · {formatPrice(item.revenue)}
            </span>
          </div>
          <div className="h-2.5 overflow-hidden rounded-full bg-muted">
            <div
              className={`animate-grow-x h-full rounded-full bg-gradient-to-r ${BAR_COLORS[index % BAR_COLORS.length]}`}
              style={{ width: `${Math.max((item.revenue / max) * 100, 3)}%`, ["--delay" as string]: `${index * 100}ms` }}
            />
          </div>
        </li>
      ))}
    </ol>
  );
}
