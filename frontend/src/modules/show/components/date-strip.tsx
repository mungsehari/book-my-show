"use client";

import { cn } from "@/lib/utils";

type Props = {
  days: string[];
  selected: string;
  onSelect: (day: string) => void;
};

function parts(iso: string) {
  const date = new Date(`${iso}T00:00:00`);
  return {
    weekday: date.toLocaleDateString("en-IN", { weekday: "short" }),
    day: date.getDate(),
    month: date.toLocaleDateString("en-IN", { month: "short" }),
  };
}

export function DateStrip({ days, selected, onSelect }: Props) {
  return (
    <div className="-mx-4 flex gap-2 overflow-x-auto px-4 pb-2">
      {days.map((iso, index) => {
        const { weekday, day, month } = parts(iso);
        const active = iso === selected;
        return (
          <button
            key={iso}
            type="button"
            onClick={() => onSelect(iso)}
            aria-pressed={active}
            className={cn(
              "flex w-16 shrink-0 flex-col items-center rounded-2xl border py-2 transition duration-200 hover:-translate-y-0.5 hover:border-primary",
              active
                ? "scale-105 border-primary bg-primary text-primary-foreground shadow-lg"
                : "bg-card",
            )}
          >
            <span className="text-xs">{index === 0 ? "Today" : weekday}</span>
            <span className="text-xl font-bold leading-tight">{day}</span>
            <span className="text-xs opacity-80">{month}</span>
          </button>
        );
      })}
    </div>
  );
}
