import { CarIcon, ClockIcon, MapPinIcon, UtensilsIcon, UsersIcon } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { formatPrice, formatTime } from "@/lib/format";
import type { PlaceSummary, PlanOption } from "@/modules/planner/types";

function Place({ icon, label, place }: { icon: React.ReactNode; label: string; place: PlaceSummary | null }) {
  return (
    <li className="flex items-start gap-3 text-sm">
      <span className="mt-0.5 text-primary">{icon}</span>
      <span>
        <span className="block text-xs uppercase tracking-wide text-muted-foreground">{label}</span>
        {place ? (
          <>
            <span className="block font-medium">{place.name}</span>
            <span className="block text-xs text-muted-foreground">
              {place.walkMinutes !== null && `${place.walkMinutes} min walk`}
              {place.notes && ` · ${place.notes}`}
            </span>
          </>
        ) : (
          <span className="block text-muted-foreground">Nothing listed near this cinema</span>
        )}
      </span>
    </li>
  );
}

export function PlanCard({ plan, people }: { plan: PlanOption; people: number }) {
  return (
    <article className="overflow-hidden rounded-3xl border bg-card shadow-md">
      <div className="bg-gradient-to-br from-rose-600 to-orange-500 p-5 text-white">
        <h3 className="text-xl font-extrabold">{plan.movieName}</h3>
        <p className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-white/90">
          <span className="flex items-center gap-1">
            <MapPinIcon className="size-4" /> {plan.cinemaName}
          </span>
          <span className="flex items-center gap-1">
            <ClockIcon className="size-4" /> {formatTime(plan.startTime)}
          </span>
        </p>
      </div>
      <div className="space-y-4 p-5">
        <ul className="space-y-3">
          <Place icon={<UtensilsIcon className="size-4" />} label="Food nearby" place={plan.food} />
          <Place icon={<CarIcon className="size-4" />} label="Parking" place={plan.parking} />
        </ul>
        <div className="flex flex-wrap items-center justify-between gap-3 border-t pt-4">
          <div>
            <p className="text-lg font-extrabold">{formatPrice(plan.totalPrice)}</p>
            <p className="flex items-center gap-1 text-xs text-muted-foreground">
              <UsersIcon className="size-3" /> {people} {people === 1 ? "person" : "people"} ·{" "}
              {formatPrice(plan.pricePerPerson)} each · {plan.seatsAvailable} seats left
            </p>
          </div>
          <Button asChild>
            <Link href={`/shows/${plan.showId}/seats`}>Pick seats</Link>
          </Button>
        </div>
      </div>
    </article>
  );
}
