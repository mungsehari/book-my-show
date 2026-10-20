import { ClockIcon } from "lucide-react";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { formatDuration } from "@/lib/format";
import { Poster } from "@/modules/movie/components/poster";
import type { Movie } from "@/modules/movie/types";

export function MovieCard({ movie }: { movie: Movie }) {
  return (
    <Link href={`/movies/${movie.id}`} className="group block">
      <article className="overflow-hidden rounded-2xl border bg-card shadow-sm transition duration-300 group-hover:-translate-y-1.5 group-hover:shadow-xl">
        <div className="relative aspect-[2/3] overflow-hidden">
          <div className="h-full w-full transition duration-500 group-hover:scale-110">
            <Poster title={movie.title} posterUrl={movie.posterUrl} />
          </div>
          <Badge className="absolute left-2 top-2 shadow">{movie.certificate}</Badge>
          <div className="absolute inset-x-0 bottom-0 translate-y-full bg-gradient-to-t from-black/80 to-transparent p-3 pt-10 transition duration-300 group-hover:translate-y-0">
            <span className="block rounded-lg bg-primary py-2 text-center text-sm font-semibold text-primary-foreground">
              Book now
            </span>
          </div>
        </div>
        <div className="space-y-1 p-3">
          <h3 className="line-clamp-1 font-semibold">{movie.title}</h3>
          <p className="line-clamp-1 text-xs text-muted-foreground">
            {movie.genre} · {movie.language}
          </p>
          <p className="flex items-center gap-1 text-xs text-muted-foreground">
            <ClockIcon className="size-3" />
            {formatDuration(movie.durationMinutes)}
          </p>
        </div>
      </article>
    </Link>
  );
}
