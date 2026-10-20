"use client";

import { CalendarRangeIcon } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { useSelectedCity } from "@/bootstrap/city-provider";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { todayIso } from "@/lib/format";
import { cn } from "@/lib/utils";
import { PlanCard } from "@/modules/planner/components/plan-card";
import { usePlanner } from "@/modules/planner/hooks";
import type { TimeSlot } from "@/modules/planner/types";

const SLOTS: { value: TimeSlot; label: string; hint: string }[] = [
  { value: "MORNING", label: "Morning", hint: "before 12" },
  { value: "AFTERNOON", label: "Afternoon", hint: "12 to 5 PM" },
  { value: "EVENING", label: "Evening", hint: "from 5 PM" },
];

export function PlannerForm() {
  const { cityId } = useSelectedCity();
  const mutation = usePlanner();
  const [date, setDate] = useState(() => todayIso());
  const [time, setTime] = useState<TimeSlot>("EVENING");
  const [people, setPeople] = useState(2);
  const [budget, setBudget] = useState("");
  const [mood, setMood] = useState("");
  const [askedPeople, setAskedPeople] = useState(2);

  if (cityId === null) {
    return <p className="text-muted-foreground">Choose a city at the top to plan your outing.</p>;
  }
  const city = cityId;

  return (
    <div className="space-y-8">
      <form
        className="space-y-5 rounded-3xl border bg-card p-5 shadow-sm"
        onSubmit={(event) => {
          event.preventDefault();
          setAskedPeople(people);
          mutation.mutate({
            cityId: city,
            date,
            time,
            people,
            mood: mood.trim() || undefined,
            budgetPerPerson: budget ? Number(budget) : undefined,
          });
        }}
      >
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="space-y-1.5 text-sm font-medium">
            Date
            <input
              type="date"
              required
              min={todayIso()}
              value={date}
              onChange={(event) => setDate(event.target.value)}
              className="h-10 w-full rounded-lg border bg-background px-3 font-normal"
            />
          </label>
          <label className="space-y-1.5 text-sm font-medium">
            People (1 to 10)
            <input
              type="number"
              min={1}
              max={10}
              required
              value={people}
              onChange={(event) => setPeople(Math.min(10, Math.max(1, Number(event.target.value) || 1)))}
              className="h-10 w-full rounded-lg border bg-background px-3 font-normal"
            />
          </label>
        </div>

        <fieldset className="space-y-2">
          <legend className="text-sm font-medium">When</legend>
          <div className="grid grid-cols-3 gap-2">
            {SLOTS.map((slot) => (
              <button
                key={slot.value}
                type="button"
                aria-pressed={time === slot.value}
                onClick={() => setTime(slot.value)}
                className={cn(
                  "rounded-xl border p-3 text-center transition hover:-translate-y-0.5",
                  time === slot.value ? "border-primary bg-primary/10" : "bg-background",
                )}
              >
                <span className="block text-sm font-medium">{slot.label}</span>
                <span className="block text-xs text-muted-foreground">{slot.hint}</span>
              </button>
            ))}
          </div>
        </fieldset>

        <div className="grid gap-4 sm:grid-cols-2">
          <label className="space-y-1.5 text-sm font-medium">
            Budget per person (optional)
            <input
              type="number"
              min={1}
              inputMode="numeric"
              value={budget}
              onChange={(event) => setBudget(event.target.value)}
              placeholder="300"
              className="h-10 w-full rounded-lg border bg-background px-3 font-normal"
            />
          </label>
          <label className="space-y-1.5 text-sm font-medium">
            Mood (optional)
            <input
              value={mood}
              maxLength={200}
              onChange={(event) => setMood(event.target.value)}
              placeholder="something light and funny"
              className="h-10 w-full rounded-lg border bg-background px-3 font-normal"
            />
          </label>
        </div>

        {mutation.error && <p className="text-sm text-destructive">{mutation.error.message}</p>}
        <Button type="submit" size="lg" disabled={mutation.isPending}>
          <CalendarRangeIcon /> {mutation.isPending ? "Planning..." : "Plan my outing"}
        </Button>
      </form>

      {mutation.isPending && (
        <div className="grid gap-4 md:grid-cols-2">
          <Skeleton className="h-56 rounded-3xl" />
          <Skeleton className="h-56 rounded-3xl" />
        </div>
      )}

      {mutation.data && (
        <section className="space-y-4">
          {mutation.data.plans.length === 0 ? (
            <p className="rounded-2xl border border-dashed p-8 text-center text-muted-foreground">
              No show fits that. Try another time, more budget, or a different day.
            </p>
          ) : (
            <>
              <h2 className="text-xl font-bold">
                {mutation.data.plans.length} {mutation.data.plans.length === 1 ? "plan" : "plans"} for you
              </h2>
              <div className="grid gap-4 md:grid-cols-2">
                {mutation.data.plans.map((plan, index) => (
                  <div
                    key={plan.showId}
                    className="animate-fade-up"
                    style={{ ["--delay" as string]: `${index * 100}ms` }}
                  >
                    <PlanCard plan={plan} people={askedPeople} />
                  </div>
                ))}
              </div>
              <p className="text-xs text-muted-foreground">
                Food and parking are added by the cinema.{" "}
                <Link href="/" className="underline">
                  Back to home
                </Link>
              </p>
            </>
          )}
        </section>
      )}
    </div>
  );
}
