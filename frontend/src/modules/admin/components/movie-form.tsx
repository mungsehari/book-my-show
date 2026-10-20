"use client";

import { useForm, useWatch } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { MovieCard } from "@/modules/movie/components/movie-card";
import { FormField } from "@/modules/auth/components/form-field";
import { ResultNote } from "@/modules/admin/components/result-note";
import { useAddMovie } from "@/modules/admin/hooks";
import type { MovieInput } from "@/modules/admin/types";

const DEFAULTS: Partial<MovieInput> = { certificate: "UA", language: "Hindi" };

export function MovieForm() {
  const { register, handleSubmit, control, reset, formState } = useForm<MovieInput>({
    defaultValues: DEFAULTS,
  });
  const mutation = useAddMovie();
  const { errors } = formState;
  const live = useWatch({ control });

  return (
    <div className="grid gap-8 lg:grid-cols-[1fr_15rem]">
      <form
        className="space-y-4"
        onSubmit={handleSubmit((values) =>
          mutation.mutate(
            {
              ...values,
              durationMinutes: Number(values.durationMinutes),
              posterUrl: values.posterUrl?.trim() || undefined,
            },
            { onSuccess: () => reset(DEFAULTS) },
          ),
        )}
      >
        <FormField
          id="movie-title"
          label="Title"
          error={errors.title?.message}
          {...register("title", { required: "Enter a title", maxLength: 200 })}
        />
        <div className="space-y-1.5">
          <label htmlFor="movie-description" className="text-sm font-medium">
            Description
          </label>
          <textarea
            id="movie-description"
            rows={3}
            maxLength={1000}
            className="w-full resize-none rounded-lg border bg-background p-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            {...register("description", { required: "Write a short description" })}
          />
          {errors.description && (
            <p className="text-xs text-destructive">{errors.description.message}</p>
          )}
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField
            id="movie-language"
            label="Language"
            error={errors.language?.message}
            {...register("language", { required: "Enter a language" })}
          />
          <FormField
            id="movie-genre"
            label="Genre"
            error={errors.genre?.message}
            {...register("genre", { required: "Enter a genre" })}
          />
          <FormField
            id="movie-duration"
            label="Duration (minutes)"
            type="number"
            error={errors.durationMinutes?.message}
            {...register("durationMinutes", {
              required: "Enter the length",
              min: { value: 1, message: "Must be positive" },
              max: { value: 600, message: "At most 600" },
            })}
          />
          <div className="space-y-1.5">
            <label htmlFor="movie-cert" className="text-sm font-medium">
              Certificate
            </label>
            <select
              id="movie-cert"
              className="h-10 w-full rounded-lg border bg-background px-3 text-sm"
              {...register("certificate")}
            >
              <option value="U">U (everyone)</option>
              <option value="UA">UA (parental guidance)</option>
              <option value="A">A (adults)</option>
            </select>
          </div>
          <FormField
            id="movie-release"
            label="Release date"
            type="date"
            error={errors.releaseDate?.message}
            {...register("releaseDate", { required: "Pick a date" })}
          />
          <FormField
            id="movie-poster"
            label="Poster link (https, optional)"
            placeholder="https://..."
            error={errors.posterUrl?.message}
            {...register("posterUrl", {
              pattern: { value: /^https:\/\/\S+$/, message: "Must start with https://" },
            })}
          />
        </div>
        <ResultNote
          success={mutation.isSuccess ? "Movie added to the catalog." : null}
          error={mutation.error?.message}
        />
        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "Saving..." : "Add movie"}
        </Button>
      </form>

      <aside className="space-y-2 lg:sticky lg:top-24 lg:self-start">
        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          Live preview
        </p>
        <MovieCard
          movie={{
            id: 0,
            title: live.title || "Movie title",
            description: live.description ?? "",
            language: live.language || "Language",
            genre: live.genre || "Genre",
            durationMinutes: Number(live.durationMinutes) || 120,
            certificate: live.certificate ?? "UA",
            releaseDate: live.releaseDate ?? "",
            posterUrl: live.posterUrl?.startsWith("https://") ? live.posterUrl : null,
          }}
        />
      </aside>
    </div>
  );
}
