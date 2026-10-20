"use client";

import { ClockIcon, MapPinIcon } from "lucide-react";
import { useState } from "react";
import { useSelectedCity } from "@/bootstrap/city-provider";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { formatDuration, todayIso } from "@/lib/format";
import { useCinemasInCity } from "@/modules/cinema/hooks";
import { Poster } from "@/modules/movie/components/poster";
import { useMovie } from "@/modules/movie/hooks";
import { FriendsActivity } from "@/modules/friend/components/friends-activity";
import { AlertForm } from "@/modules/alert/components/alert-form";
import { ReviewsSection } from "@/modules/review/components/reviews-section";
import { DateStrip } from "@/modules/show/components/date-strip";
import { ShowtimesByCinema } from "@/modules/show/components/showtimes-by-cinema";

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

export function MovieDetail({ movieId }: { movieId: number }) {
  const { cityId } = useSelectedCity();
  const movie = useMovie(movieId);
  const cinemas = useCinemasInCity(cityId);
  const [days] = useState(() => nextDays(7));
  const [date, setDate] = useState(days[0]);

  if (movie.isLoading) return <Skeleton className="h-72 w-full rounded-3xl" />;
  if (movie.error || !movie.data) {
    return (
      <p className="text-destructive">{movie.error?.message ?? "Movie not found."}</p>
    );
  }
  const data = movie.data;

  return (
    <div className="space-y-10">
      <section className="animate-fade-up relative overflow-hidden rounded-3xl bg-gradient-to-br from-zinc-900 via-zinc-800 to-rose-950 p-6 text-white shadow-xl sm:p-10">
        <div className="relative flex flex-col gap-6 sm:flex-row sm:items-end">
          <div className="aspect-[2/3] w-40 shrink-0 overflow-hidden rounded-2xl shadow-2xl ring-1 ring-white/20 sm:w-52">
            <Poster title={data.title} posterUrl={data.posterUrl} />
          </div>
          <div className="space-y-3">
            <h1 className="text-3xl font-extrabold sm:text-4xl">{data.title}</h1>
            <div className="flex flex-wrap items-center gap-2">
              <Badge>{data.certificate}</Badge>
              <Badge variant="secondary">{data.language}</Badge>
              <Badge variant="secondary">{data.genre}</Badge>
              <span className="flex items-center gap-1 text-sm text-white/80">
                <ClockIcon className="size-4" />
                {formatDuration(data.durationMinutes)}
              </span>
            </div>
            <p className="max-w-2xl text-white/80">{data.description}</p>
          </div>
        </div>
      </section>

      <FriendsActivity movieId={movieId} />

      <section className="space-y-4">
        <h2 className="text-2xl font-bold tracking-tight">Pick a show</h2>
        <DateStrip days={days} selected={date} onSelect={setDate} />
        {cityId === null ? (
          <div className="flex flex-col items-center gap-2 rounded-2xl border border-dashed py-12 text-center text-muted-foreground">
            <MapPinIcon className="size-9 text-primary" />
            <p>Choose a city at the top right to see showtimes.</p>
          </div>
        ) : cinemas.isLoading ? (
          <Skeleton className="h-32 w-full rounded-2xl" />
        ) : (
          <ShowtimesByCinema
            cinemas={cinemas.data?.items ?? []}
            movieId={movieId}
            date={date}
          />
        )}
      </section>

      <AlertForm movieId={movieId} />

      <ReviewsSection movieId={movieId} />
    </div>
  );
}
