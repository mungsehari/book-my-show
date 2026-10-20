"use client";

import { SearchIcon, SparklesIcon, XIcon } from "lucide-react";
import { useState } from "react";
import { useSelectedCity } from "@/bootstrap/city-provider";
import { Button } from "@/components/ui/button";
import { MovieGrid } from "@/modules/movie/components/movie-grid";
import { useMoodSearch } from "@/modules/discover/hooks";
import type { MoodSearchResult } from "@/modules/discover/types";

const IDEAS = [
  "something light and funny",
  "thriller under 2 hours",
  "family movie for kids",
  "kuch halka funny, 2 ghante se kam",
];

function describe(result: MoodSearchResult): string {
  const { filters } = result;
  const parts = [
    ...filters.genres,
    ...filters.languages,
    filters.maxDurationMinutes ? `under ${filters.maxDurationMinutes} min` : "",
    ...filters.certificates,
  ].filter(Boolean);
  return parts.length > 0 ? parts.join(" · ") : "all movies";
}

export function MoodSearch() {
  const { cityId } = useSelectedCity();
  const [text, setText] = useState("");
  const [submitted, setSubmitted] = useState("");
  const { data, isFetching, error } = useMoodSearch(submitted, cityId);

  function run(value: string) {
    const trimmed = value.trim();
    setText(value);
    setSubmitted(trimmed);
  }

  return (
    <section className="space-y-4">
      <form
        className="relative"
        onSubmit={(event) => {
          event.preventDefault();
          run(text);
        }}
      >
        <SparklesIcon className="absolute left-4 top-1/2 size-5 -translate-y-1/2 text-primary" />
        <input
          value={text}
          onChange={(event) => setText(event.target.value)}
          maxLength={200}
          placeholder="What do you feel like watching? Try “something light and funny”"
          aria-label="Describe the movie you want"
          className="h-14 w-full rounded-full border bg-card pl-12 pr-32 text-sm shadow-sm outline-none transition focus:ring-2 focus:ring-primary sm:text-base"
        />
        <div className="absolute right-2 top-1/2 flex -translate-y-1/2 items-center gap-1">
          {submitted && (
            <Button type="button" variant="ghost" size="icon" aria-label="Clear search" onClick={() => run("")}>
              <XIcon />
            </Button>
          )}
          <Button type="submit" className="rounded-full" disabled={!text.trim() || isFetching}>
            <SearchIcon /> Find
          </Button>
        </div>
      </form>

      {!submitted && (
        <div className="flex flex-wrap gap-2">
          {IDEAS.map((idea) => (
            <button
              key={idea}
              type="button"
              onClick={() => run(idea)}
              className="rounded-full border bg-card px-3 py-1.5 text-xs text-muted-foreground transition hover:-translate-y-0.5 hover:border-primary hover:text-foreground"
            >
              {idea}
            </button>
          ))}
        </div>
      )}

      {submitted && (
        <div className="animate-fade-up space-y-3">
          {error && <p className="text-destructive">{error.message}</p>}
          {data && (
            <p className="text-sm text-muted-foreground">
              <span className="mr-2 rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
                {data.source === "claude" ? "AI picked" : "Smart match"}
              </span>
              Showing {describe(data)} for “{data.query}”
            </p>
          )}
          <MovieGrid movies={data?.movies} isLoading={isFetching && !data} />
        </div>
      )}
    </section>
  );
}
