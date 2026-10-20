import { useCallback, useMemo, useState } from "react";
import { MAX_SEATS, type SeatMap, type SeatMapSeat } from "@/modules/seat/types";

/** Keeps the chosen seat ids. Seats that stop being available are dropped. */
export function useSeatSelection(seatMap: SeatMap | undefined) {
  const [chosen, setChosen] = useState<number[]>([]);
  const [notice, setNotice] = useState<string | null>(null);

  const seatsById = useMemo(() => {
    const map = new Map<number, SeatMapSeat & { label: string }>();
    for (const row of seatMap?.rows ?? []) {
      for (const seat of row.seats) {
        map.set(seat.id, { ...seat, label: `${row.label}${seat.number}` });
      }
    }
    return map;
  }, [seatMap]);

  // Only seats that are still free count. This also hides seats taken by others.
  const selectedIds = useMemo(
    () => chosen.filter((id) => seatsById.get(id)?.status === "AVAILABLE"),
    [chosen, seatsById],
  );
  const lostCount = chosen.length - selectedIds.length;

  const toggle = useCallback(
    (seat: SeatMapSeat) => {
      if (seat.status !== "AVAILABLE") return;
      setNotice(null);
      if (selectedIds.includes(seat.id)) {
        setChosen(selectedIds.filter((id) => id !== seat.id));
      } else if (selectedIds.length >= MAX_SEATS) {
        setNotice(`You can pick up to ${MAX_SEATS} seats.`);
      } else {
        setChosen([...selectedIds, seat.id]);
      }
    },
    [selectedIds],
  );

  const clear = useCallback(() => setChosen([]), []);

  const selected = selectedIds.map((id) => seatsById.get(id)!);
  const total = selected.reduce((sum, seat) => sum + Number(seat.price ?? 0), 0);

  return { selected, selectedIds, total, toggle, clear, notice, lostCount };
}
