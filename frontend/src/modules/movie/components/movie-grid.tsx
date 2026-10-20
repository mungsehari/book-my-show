import { FilmIcon } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { MovieCard } from "@/modules/movie/components/movie-card";
import type { Movie } from "@/modules/movie/types";

const GRID = "grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5";

export function MovieGrid({
  movies,
  isLoading,
}: {
  movies?: Movie[];
  isLoading: boolean;
}) {
  if (isLoading) {
    return (
      <div className={GRID}>
        {Array.from({ length: 5 }, (_, i) => (
          <Skeleton key={i} className="aspect-[2/3] w-full rounded-2xl" />
        ))}
      </div>
    );
  }
  if (!movies || movies.length === 0) {
    return (
      <div className="flex flex-col items-center gap-2 rounded-2xl border border-dashed py-14 text-center text-muted-foreground">
        <FilmIcon className="size-10" />
        <p>No movies playing in this city right now.</p>
        <p className="text-sm">Try another city from the top right.</p>
      </div>
    );
  }
  return (
    <div className={GRID}>
      {movies.map((movie, index) => (
        <div
          key={movie.id}
          className="animate-fade-up"
          style={{ ["--delay" as string]: `${index * 70}ms` }}
        >
          <MovieCard movie={movie} />
        </div>
      ))}
    </div>
  );
}
