"use client";

import { BadgeCheckIcon } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { ReviewForm } from "@/modules/review/components/review-form";
import { Stars } from "@/modules/review/components/star-rating";
import { useReviews } from "@/modules/review/hooks";

export function ReviewsSection({ movieId }: { movieId: number }) {
  const { data, isLoading, error } = useReviews(movieId);

  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-baseline gap-3">
        <h2 className="text-2xl font-bold tracking-tight">Reviews</h2>
        {data && data.averageRating !== null && (
          <span className="flex items-center gap-2 text-sm text-muted-foreground">
            <Stars value={data.averageRating} />
            <strong className="text-foreground">{data.averageRating}</strong>
            ({data.reviewCount})
          </span>
        )}
      </div>

      <ReviewForm movieId={movieId} />

      {isLoading && <Skeleton className="h-24 w-full rounded-2xl" />}
      {error && <p className="text-destructive">{error.message}</p>}
      {data && data.items.length === 0 && (
        <p className="text-sm text-muted-foreground">
          No reviews yet. Watch it and be the first.
        </p>
      )}
      <ul className="space-y-3">
        {data?.items.map((review, index) => (
          <li
            key={review.id}
            className="animate-fade-up rounded-2xl border bg-card p-4"
            style={{ ["--delay" as string]: `${index * 50}ms` }}
          >
            <div className="flex items-center justify-between gap-2">
              <span className="flex items-center gap-1.5 text-sm font-medium">
                {review.userName}
                {review.verified && (
                  <span className="flex items-center gap-0.5 text-xs text-emerald-600">
                    <BadgeCheckIcon className="size-3.5" /> Verified viewer
                  </span>
                )}
              </span>
              <Stars value={review.rating} />
            </div>
            {review.text && <p className="mt-2 text-sm text-muted-foreground">{review.text}</p>}
          </li>
        ))}
      </ul>
    </section>
  );
}
