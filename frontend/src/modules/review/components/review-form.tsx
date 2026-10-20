"use client";

import Link from "next/link";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { useMe } from "@/modules/auth/hooks";
import { StarInput } from "@/modules/review/components/star-rating";
import { usePostReview } from "@/modules/review/hooks";

export function ReviewForm({ movieId }: { movieId: number }) {
  const { data: user } = useMe();
  const mutation = usePostReview(movieId);
  const [rating, setRating] = useState(0);
  const [text, setText] = useState("");

  if (!user) {
    return (
      <p className="text-sm text-muted-foreground">
        <Link href={`/login?next=/movies/${movieId}`} className="text-primary underline">
          Sign in
        </Link>{" "}
        to review this movie. Only people who watched it can post.
      </p>
    );
  }

  return (
    <form
      className="space-y-3 rounded-2xl border bg-card p-4"
      onSubmit={(event) => {
        event.preventDefault();
        mutation.mutate(
          { rating, text: text.trim() || undefined },
          {
            onSuccess: () => {
              setRating(0);
              setText("");
            },
          },
        );
      }}
    >
      <p className="text-sm font-medium">Rate this movie</p>
      <StarInput value={rating} onChange={setRating} />
      <textarea
        value={text}
        onChange={(event) => setText(event.target.value)}
        maxLength={1000}
        rows={3}
        placeholder="What did you think? (optional)"
        className="w-full resize-none rounded-lg border bg-background p-3 text-sm outline-none focus:ring-2 focus:ring-ring"
      />
      {mutation.error && (
        <p className="text-sm text-destructive">{mutation.error.message}</p>
      )}
      {mutation.isSuccess && (
        <p className="animate-pop-in text-sm text-emerald-600">Thanks! Your review is posted.</p>
      )}
      <Button type="submit" disabled={rating === 0 || mutation.isPending}>
        {mutation.isPending ? "Posting..." : "Post review"}
      </Button>
    </form>
  );
}
