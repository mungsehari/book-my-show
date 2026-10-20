"use client";

import { CalendarX2Icon, MapPinIcon } from "lucide-react";
import Link from "next/link";
import { Skeleton } from "@/components/ui/skeleton";
import { formatPrice, formatTime } from "@/lib/format";
import type { Cinema } from "@/modules/cinema/types";
import { useMovieShows } from "@/modules/show/hooks";
import type { Show } from "@/modules/show/types";

type Props = {
  cinemas: Cinema[];
  movieId: number;
  date: string;
};

function tagsOf(show: Show): string[] {
  const tags: string[] = [];
  if (show.quiet) tags.push("Quiet");
  if (show.subtitles) tags.push("Subtitles");
  if (show.sensoryFriendly) tags.push("Sensory friendly");
  return tags;
}

export function ShowtimesByCinema({ cinemas, movieId, date }: Props) {
  const cinemaIds = cinemas.map((cinema) => cinema.id);
  const { groups, isLoading, error } = useMovieShows(cinemaIds, movieId, date);

  if (isLoading) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-28 w-full rounded-2xl" />
        <Skeleton className="h-28 w-full rounded-2xl" />
      </div>
    );
  }
  if (error) return <p className="text-destructive">{error.message}</p>;
  if (groups.length === 0) {
    return (
      <div className="flex flex-col items-center gap-2 rounded-2xl border border-dashed py-12 text-center text-muted-foreground">
        <CalendarX2Icon className="size-9" />
        <p>No shows on this date.</p>
        <p className="text-sm">Try another day from the strip above.</p>
      </div>
    );
  }

  return (
    <ul className="space-y-4" key={date}>
      {groups.map((group, index) => {
        const cinema = cinemas.find((item) => item.id === group.cinemaId);
        return (
          <li
            key={group.cinemaId}
            className="animate-fade-up rounded-2xl border bg-card p-4 shadow-sm"
            style={{ ["--delay" as string]: `${index * 80}ms` }}
          >
            <h3 className="font-semibold">{cinema?.name}</h3>
            <p className="mb-3 flex items-center gap-1 text-xs text-muted-foreground">
              <MapPinIcon className="size-3" />
              {cinema?.address}
            </p>
            <div className="flex flex-wrap gap-3">
              {group.shows.map((show) => (
                <Link
                  key={show.id}
                  href={`/shows/${show.id}/seats`}
                  className="group rounded-xl border border-emerald-500/60 px-4 py-2 text-center transition duration-200 hover:-translate-y-0.5 hover:bg-emerald-500 hover:text-white hover:shadow-md"
                >
                  <span className="block text-sm font-semibold text-emerald-600 group-hover:text-white dark:text-emerald-400">
                    {formatTime(show.startTime)}
                  </span>
                  <span className="block text-xs text-muted-foreground">
                    {formatPrice(show.price)}
                  </span>
                  {tagsOf(show).length > 0 && (
                    <span className="mt-0.5 block text-[10px] uppercase tracking-wide text-muted-foreground">
                      {tagsOf(show).join(" · ")}
                    </span>
                  )}
                </Link>
              ))}
            </div>
          </li>
        );
      })}
    </ul>
  );
}
