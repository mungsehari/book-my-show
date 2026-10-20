"use client";

import { MapPinIcon } from "lucide-react";
import { useSelectedCity } from "@/bootstrap/city-provider";
import { MovieGrid } from "@/modules/movie/components/movie-grid";
import { useMoviesInCity } from "@/modules/movie/hooks";

export function MoviesInCity() {
  const { cityId } = useSelectedCity();
  const { data, isLoading, error } = useMoviesInCity(cityId);

  if (cityId === null) {
    return (
      <div className="flex flex-col items-center gap-2 rounded-2xl border border-dashed py-14 text-center text-muted-foreground">
        <MapPinIcon className="size-10 text-primary" />
        <p className="font-medium text-foreground">Where do you want to watch?</p>
        <p className="text-sm">Pick your city at the top right to see what is playing.</p>
      </div>
    );
  }
  if (error) {
    return <p className="py-10 text-center text-destructive">{error.message}</p>;
  }
  return <MovieGrid movies={data?.items} isLoading={isLoading} />;
}
