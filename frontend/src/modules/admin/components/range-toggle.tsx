"use client";

import { cn } from "@/lib/utils";
import { RANGES, type Range } from "@/modules/admin/types";

type Props = { value: Range; onChange: (value: Range) => void };

export function RangeToggle({ value, onChange }: Props) {
  return (
    <div className="inline-flex gap-1 rounded-xl bg-muted p-1" role="tablist" aria-label="Time range">
      {RANGES.map((range) => (
        <button
          key={range}
          type="button"
          role="tab"
          aria-selected={value === range}
          onClick={() => onChange(range)}
          className={cn(
            "rounded-lg px-3 py-1.5 text-sm font-medium transition",
            value === range ? "bg-background shadow" : "text-muted-foreground",
          )}
        >
          {range} days
        </button>
      ))}
    </div>
  );
}
