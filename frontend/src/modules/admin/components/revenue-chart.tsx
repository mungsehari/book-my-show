"use client";

import { useState } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { formatPrice } from "@/lib/format";
import { useDaily } from "@/modules/admin/hooks";

const W = 600;
const H = 220;
const PAD = { left: 8, right: 8, top: 16, bottom: 24 };

function shortDate(iso: string): string {
  return new Date(`${iso}T00:00:00`).toLocaleDateString("en-IN", { day: "numeric", month: "short" });
}

/** Revenue per day as a line with a soft fill. Drawn with SVG, no chart library. */
export function RevenueChart({ days }: { days: number }) {
  const { data, isLoading, error } = useDaily(days);
  const [active, setActive] = useState<number | null>(null);

  if (isLoading) return <Skeleton className="h-64 w-full rounded-2xl" />;
  if (error || !data) return <p className="text-sm text-destructive">{error?.message}</p>;

  const points = data.points;
  const max = Math.max(...points.map((p) => p.revenue), 1);
  const innerW = W - PAD.left - PAD.right;
  const innerH = H - PAD.top - PAD.bottom;
  const x = (i: number) => PAD.left + (points.length === 1 ? innerW / 2 : (i / (points.length - 1)) * innerW);
  const y = (value: number) => PAD.top + innerH - (value / max) * innerH;

  const line = points.map((p, i) => `${i === 0 ? "M" : "L"}${x(i).toFixed(1)},${y(p.revenue).toFixed(1)}`).join(" ");
  const area = `${line} L${x(points.length - 1).toFixed(1)},${H - PAD.bottom} L${x(0).toFixed(1)},${H - PAD.bottom} Z`;
  const total = points.reduce((sum, p) => sum + p.revenue, 0);
  const current = active === null ? null : points[active];

  return (
    <div className="space-y-2">
      <div className="flex min-h-10 items-baseline justify-between gap-3">
        <p className="text-sm text-muted-foreground">
          {current ? shortDate(current.date) : `Last ${days} days`}
        </p>
        <p className="text-right text-sm">
          <strong className="text-base">{formatPrice(current ? current.revenue : total)}</strong>
          {current && (
            <span className="ml-2 text-muted-foreground">
              {current.tickets} {current.tickets === 1 ? "ticket" : "tickets"}
            </span>
          )}
        </p>
      </div>

      {total === 0 && (
        <p className="rounded-xl bg-muted p-3 text-center text-sm text-muted-foreground">
          No sales in this period yet. The line shows up after the first booking.
        </p>
      )}

      <svg
        viewBox={`0 0 ${W} ${H}`}
        className="w-full overflow-visible"
        role="img"
        aria-label={`Revenue per day for the last ${days} days`}
        onMouseLeave={() => setActive(null)}
      >
        <defs>
          <linearGradient id="revenue-fill" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stopColor="var(--primary)" stopOpacity="0.28" />
            <stop offset="100%" stopColor="var(--primary)" stopOpacity="0" />
          </linearGradient>
        </defs>
        {[0, 0.5, 1].map((fraction) => (
          <line
            key={fraction}
            x1={PAD.left}
            x2={W - PAD.right}
            y1={PAD.top + innerH * fraction}
            y2={PAD.top + innerH * fraction}
            stroke="var(--border)"
            strokeDasharray="4 4"
          />
        ))}
        <path d={area} fill="url(#revenue-fill)" className="animate-pop-in" />
        <path
          d={line}
          pathLength={1}
          fill="none"
          stroke="var(--primary)"
          strokeWidth={2.5}
          strokeLinecap="round"
          strokeLinejoin="round"
          className="animate-draw"
        />
        {active !== null && (
          <>
            <line x1={x(active)} x2={x(active)} y1={PAD.top} y2={H - PAD.bottom} stroke="var(--primary)" strokeOpacity={0.4} />
            <circle cx={x(active)} cy={y(points[active].revenue)} r={5} fill="var(--primary)" />
          </>
        )}
        {points.map((p, i) => (
          <rect
            key={p.date}
            x={x(i) - innerW / points.length / 2}
            y={PAD.top}
            width={innerW / points.length}
            height={innerH}
            fill="transparent"
            onMouseEnter={() => setActive(i)}
            onTouchStart={() => setActive(i)}
          />
        ))}
        <text x={PAD.left} y={H - 6} fontSize={11} fill="var(--muted-foreground)">
          {shortDate(points[0].date)}
        </text>
        <text x={W - PAD.right} y={H - 6} fontSize={11} textAnchor="end" fill="var(--muted-foreground)">
          {shortDate(points[points.length - 1].date)}
        </text>
      </svg>
    </div>
  );
}
