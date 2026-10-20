import { cn } from "@/lib/utils";

const ITEMS = [
  { label: "Available", className: "border-emerald-500/70" },
  { label: "Selected", className: "border-primary bg-primary" },
  { label: "Held by someone", className: "border-amber-400/60 bg-amber-400/30" },
  { label: "Sold", className: "border-transparent bg-muted" },
];

export function SeatLegend() {
  return (
    <ul className="flex flex-wrap justify-center gap-x-5 gap-y-2 text-xs text-muted-foreground">
      {ITEMS.map((item) => (
        <li key={item.label} className="flex items-center gap-1.5">
          <span className={cn("size-4 rounded border", item.className)} />
          {item.label}
        </li>
      ))}
    </ul>
  );
}
