"use client";

import { CheckIcon } from "lucide-react";
import { useState } from "react";
import { useSelectedCity } from "@/bootstrap/city-provider";
import { Skeleton } from "@/components/ui/skeleton";
import { formatTime, todayIso } from "@/lib/format";
import { cn } from "@/lib/utils";
import { useCinemasInCity } from "@/modules/cinema/hooks";
import { DateStrip } from "@/modules/show/components/date-strip";
import { useShowsOnDate } from "@/modules/show/hooks";
import { MAX_POLL_SHOWS } from "@/modules/poll/types";

export type PickedShow = { id: number; label: string };

type Props = {
  picked: PickedShow[];
  onToggle: (show: PickedShow) => void;
};

function nextDays(count: number): string[] {
  const start = new Date(`${todayIso()}T00:00:00`);
  return Array.from({ length: count }, (_, i) => {
    const day = new Date(start);
    day.setDate(start.getDate() + i);
    const month = String(day.getMonth() + 1).padStart(2, "0");
    const date = String(day.getDate()).padStart(2, "0");
    return `${day.getFullYear()}-${month}-${date}`;
  });
}

/** Lists the shows of the chosen city for one day. Tap to add or remove. */
export function ShowPicker({ picked, onToggle }: Props) {
  const { cityId } = useSelectedCity();
  const cinemas = useCinemasInCity(cityId);
  const [days] = useState(() => nextDays(7));
  const [date, setDate] = useState(days[0]);
  const items = cinemas.data?.items ?? [];
  const { shows, isLoading } = useShowsOnDate(
    items.map((cinema) => cinema.id),
    date,
  );

  if (cityId === null) {
    return <p className="text-sm text-muted-foreground">Choose a city at the top to pick shows.</p>;
  }

  return (
    <div className="space-y-3">
      <DateStrip days={days} selected={date} onSelect={setDate} />
      {(cinemas.isLoading || isLoading) && <Skeleton className="h-24 w-full rounded-2xl" />}
      {!isLoading && shows.length === 0 && (
        <p className="text-sm text-muted-foreground">No shows on this date.</p>
      )}
      <ul className="grid gap-2 sm:grid-cols-2">
        {shows.map((show) => {
          const cinema = items.find((item) => item.id === show.cinemaId);
          const isPicked = picked.some((item) => item.id === show.id);
          const full = !isPicked && picked.length >= MAX_POLL_SHOWS;
          const label = `${show.movieName ?? "Movie"} · ${cinema?.name ?? "Cinema"} · ${new Date(`${show.showDate}T00:00:00`).toLocaleDateString("en-IN", { day: "numeric", month: "short" })} ${formatTime(show.startTime)}`;
          return (
            <li key={show.id}>
              <button
                type="button"
                disabled={full}
                aria-pressed={isPicked}
                onClick={() => onToggle({ id: show.id, label })}
                className={cn(
                  "flex w-full items-center justify-between gap-2 rounded-xl border p-3 text-left text-sm transition hover:-translate-y-0.5 disabled:opacity-40",
                  isPicked ? "border-primary bg-primary/10" : "bg-card",
                )}
              >
                <span>
                  <span className="block font-medium">{show.movieName}</span>
                  <span className="block text-xs text-muted-foreground">
                    {cinema?.name} · {formatTime(show.startTime)}
                  </span>
                </span>
                {isPicked && <CheckIcon className="size-5 text-primary" />}
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
