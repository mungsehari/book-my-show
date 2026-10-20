import { cn } from "@/lib/utils";
import { CATEGORY_STYLE, type RowInput } from "@/modules/admin/types";

/** Draws the seats as the builder rows describe them. */
export function ScreenPreview({ rows }: { rows: RowInput[] }) {
  const total = rows.reduce((sum, row) => sum + (row.seatCount || 0), 0);
  let seatIndex = 0;

  return (
    <div className="space-y-3 rounded-2xl border bg-card p-4">
      <div className="space-y-1 text-center">
        <div className="mx-auto h-1.5 w-2/3 rounded-full bg-gradient-to-r from-transparent via-primary/60 to-transparent" />
        <p className="text-[10px] uppercase tracking-widest text-muted-foreground">Screen</p>
      </div>
      <div className="overflow-x-auto">
        <div className="mx-auto w-max space-y-1.5">
          {rows.map((row) => (
            <div key={row.label} className="flex items-center gap-1.5">
              <span className="w-4 text-center text-xs text-muted-foreground">{row.label}</span>
              <div className="flex gap-1">
                {Array.from({ length: Math.min(row.seatCount || 0, 50) }, (_, i) => {
                  const number = i + 1;
                  const wheelchair = row.wheelchairSeats?.includes(number);
                  const delay = Math.min(seatIndex++ * 8, 600);
                  return (
                    <span
                      key={number}
                      title={`${row.label}${number}`}
                      className={cn(
                        "animate-pop-in flex size-5 items-center justify-center rounded text-[8px]",
                        wheelchair ? "bg-blue-600 text-white" : CATEGORY_STYLE[row.category],
                      )}
                      style={{ animationDelay: `${delay}ms` }}
                    >
                      {wheelchair ? "♿" : ""}
                    </span>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </div>
      <p className="text-center text-sm font-medium">{total} seats</p>
    </div>
  );
}
