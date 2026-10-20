import { formatPrice } from "@/lib/format";
import { SeatButton } from "@/modules/seat/components/seat-button";
import type { SeatMap, SeatMapSeat } from "@/modules/seat/types";

type Props = {
  seatMap: SeatMap;
  selectedIds: number[];
  onToggle: (seat: SeatMapSeat) => void;
};

/** Rows grouped by category, each group has a price heading. */
export function SeatGrid({ seatMap, selectedIds, onToggle }: Props) {
  const groups: { category: string; rows: SeatMap["rows"] }[] = [];
  for (const row of seatMap.rows) {
    const category = row.seats[0]?.category ?? "";
    const last = groups[groups.length - 1];
    if (last && last.category === category) last.rows.push(row);
    else groups.push({ category, rows: [row] });
  }

  return (
    <div className="overflow-x-auto pb-2">
      <div className="mx-auto w-max space-y-6 px-2">
        {groups.map((group) => {
          const price = seatMap.prices.find((item) => item.category === group.category);
          return (
            <section key={group.category} className="space-y-2">
              <h3 className="text-center text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                {group.category}
                {price && ` · ${formatPrice(price.price)}`}
                {price && price.originalPrice > price.price && (
                  <span className="ml-1 font-normal line-through">
                    {formatPrice(price.originalPrice)}
                  </span>
                )}
              </h3>
              {group.rows.map((row) => (
                <div key={row.label} className="flex items-center gap-2">
                  <span className="w-5 text-center text-xs text-muted-foreground">
                    {row.label}
                  </span>
                  <div className="flex gap-1.5">
                    {row.seats.map((seat) => (
                      <SeatButton
                        key={seat.id}
                        seat={seat}
                        label={`${row.label}${seat.number}`}
                        selected={selectedIds.includes(seat.id)}
                        onToggle={onToggle}
                      />
                    ))}
                  </div>
                </div>
              ))}
            </section>
          );
        })}
      </div>
    </div>
  );
}
