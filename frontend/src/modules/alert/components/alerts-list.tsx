"use client";

import { BellOffIcon } from "lucide-react";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { formatPrice } from "@/lib/format";
import { useCancelAlert, useMyAlerts } from "@/modules/alert/hooks";

const VARIANT: Record<string, "default" | "secondary" | "outline"> = {
  ACTIVE: "default",
  TRIGGERED: "secondary",
  CANCELLED: "outline",
};

export function AlertsList() {
  const { data, isLoading, error } = useMyAlerts();
  const cancel = useCancelAlert();

  if (isLoading) return <Skeleton className="h-24 w-full rounded-2xl" />;
  if (error) return <p className="text-destructive">{error.message}</p>;
  if (!data || data.items.length === 0) {
    return (
      <div className="flex flex-col items-center gap-2 rounded-2xl border border-dashed py-14 text-center text-muted-foreground">
        <BellOffIcon className="size-10" />
        <p>No price alerts yet.</p>
        <p className="text-sm">Open a movie and set one to hear when tickets get cheaper.</p>
      </div>
    );
  }

  return (
    <ul className="space-y-3">
      {data.items.map((alert, index) => (
        <li
          key={alert.id}
          className="animate-fade-up flex flex-wrap items-center justify-between gap-3 rounded-2xl border bg-card p-4"
          style={{ ["--delay" as string]: `${index * 50}ms` }}
        >
          <div className="space-y-1">
            <Link href={`/movies/${alert.movieId}`} className="font-semibold hover:underline">
              Movie #{alert.movieId}
            </Link>
            <p className="text-sm text-muted-foreground">
              Alert at {formatPrice(alert.maxPrice)} or less
            </p>
            {alert.status === "TRIGGERED" && alert.triggeredShowId && (
              <p className="text-sm text-emerald-600">
                Found {alert.triggeredPrice ? formatPrice(alert.triggeredPrice) : "a cheaper ticket"}.{" "}
                <Link href={`/shows/${alert.triggeredShowId}/seats`} className="underline">
                  Book now
                </Link>
              </p>
            )}
          </div>
          <div className="flex items-center gap-2">
            <Badge variant={VARIANT[alert.status] ?? "outline"}>{alert.status}</Badge>
            {alert.status === "ACTIVE" && (
              <Button
                size="sm"
                variant="outline"
                disabled={cancel.isPending}
                onClick={() => cancel.mutate(alert.id)}
              >
                Cancel
              </Button>
            )}
          </div>
        </li>
      ))}
    </ul>
  );
}
