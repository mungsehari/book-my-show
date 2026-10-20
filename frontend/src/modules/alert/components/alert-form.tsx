"use client";

import { BellIcon } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { useSelectedCity } from "@/bootstrap/city-provider";
import { Button } from "@/components/ui/button";
import { useMe } from "@/modules/auth/hooks";
import { useCreateAlert } from "@/modules/alert/hooks";

/** "Tell me when this movie costs this much or less" for the chosen city. */
export function AlertForm({ movieId }: { movieId: number }) {
  const { cityId } = useSelectedCity();
  const { data: user } = useMe();
  const mutation = useCreateAlert();
  const [price, setPrice] = useState("");

  if (!user) {
    return (
      <p className="text-sm text-muted-foreground">
        <Link href={`/login?next=/movies/${movieId}`} className="text-primary underline">
          Sign in
        </Link>{" "}
        to get a price alert.
      </p>
    );
  }
  if (cityId === null) {
    return <p className="text-sm text-muted-foreground">Choose a city to set a price alert.</p>;
  }

  const amount = Number(price);
  const valid = Number.isFinite(amount) && amount >= 1;

  return (
    <form
      className="flex flex-wrap items-center gap-3 rounded-2xl border bg-card p-4"
      onSubmit={(event) => {
        event.preventDefault();
        mutation.mutate({ movieId, cityId, maxPrice: amount }, { onSuccess: () => setPrice("") });
      }}
    >
      <BellIcon className="size-5 text-primary" />
      <label htmlFor="alert-price" className="text-sm font-medium">
        Tell me when a ticket costs
      </label>
      <div className="flex items-center rounded-lg border bg-background px-3">
        <span className="text-muted-foreground">₹</span>
        <input
          id="alert-price"
          type="number"
          min={1}
          step="1"
          inputMode="numeric"
          value={price}
          onChange={(event) => setPrice(event.target.value)}
          placeholder="200"
          className="w-24 bg-transparent px-2 py-2 text-sm outline-none"
        />
      </div>
      <span className="text-sm text-muted-foreground">or less</span>
      <Button type="submit" size="sm" disabled={!valid || mutation.isPending}>
        {mutation.isPending ? "Saving..." : "Set alert"}
      </Button>
      {mutation.isSuccess && (
        <span className="animate-pop-in text-sm text-emerald-600">
          Alert set.{" "}
          <Link href="/alerts" className="underline">
            See my alerts
          </Link>
        </span>
      )}
      {mutation.error && (
        <span className="w-full text-sm text-destructive">{mutation.error.message}</span>
      )}
    </form>
  );
}
