import { AccessibilityIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import type { SeatMapSeat } from "@/modules/seat/types";

type Props = {
  seat: SeatMapSeat;
  label: string;
  selected: boolean;
  onToggle: (seat: SeatMapSeat) => void;
};

const STATUS_STYLE: Record<string, string> = {
  AVAILABLE:
    "border-emerald-500/70 text-emerald-700 hover:-translate-y-0.5 hover:bg-emerald-500 hover:text-white dark:text-emerald-300",
  LOCKED: "cursor-not-allowed border-amber-400/60 bg-amber-400/30 text-amber-700 dark:text-amber-300",
  BOOKED: "cursor-not-allowed border-transparent bg-muted text-muted-foreground/50",
  UNAVAILABLE: "invisible",
};

export function SeatButton({ seat, label, selected, onToggle }: Props) {
  const disabled = seat.status !== "AVAILABLE";
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={() => onToggle(seat)}
      aria-pressed={selected}
      aria-label={`Seat ${label}, ${seat.status.toLowerCase()}${seat.wheelchair ? ", wheelchair" : ""}`}
      title={`${label}${seat.price ? ` · ₹${seat.price}` : ""}`}
      className={cn(
        "flex size-8 shrink-0 items-center justify-center rounded-md border text-[10px] font-medium transition duration-150 sm:size-9",
        STATUS_STYLE[seat.status],
        selected &&
          "animate-pop-in border-primary bg-primary text-primary-foreground shadow-md hover:bg-primary hover:text-primary-foreground",
      )}
    >
      {seat.wheelchair ? <AccessibilityIcon className="size-4" /> : seat.number}
    </button>
  );
}
