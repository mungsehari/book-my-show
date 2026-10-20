"use client";

import { CheckIcon } from "lucide-react";
import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { todayIso } from "@/lib/format";
import { cn } from "@/lib/utils";
import { NativeSelect } from "@/modules/admin/components/native-select";
import { ResultNote } from "@/modules/admin/components/result-note";
import { ScreenSelect } from "@/modules/admin/components/screen-select";
import { useAddShow, useLayout } from "@/modules/admin/hooks";
import { CATEGORY_LABEL, type SeatCategory } from "@/modules/admin/types";
import { useAllMovies } from "@/modules/movie/hooks";

const STEPS = ["Where", "What and when", "Prices"];

const FLAGS = [
  { key: "quiet", label: "Quiet show", hint: "Fewer kids, calmer crowd" },
  { key: "sensoryFriendly", label: "Sensory friendly", hint: "Lower sound, softer lights" },
  { key: "subtitles", label: "Subtitles", hint: "On-screen subtitles" },
  { key: "lastMinuteDeals", label: "Last-minute deals", hint: "20% off in the last hour if seats are empty" },
] as const;

type FlagKey = (typeof FLAGS)[number]["key"];

export function ShowWizard() {
  const [step, setStep] = useState(0);
  const [screenId, setScreenId] = useState<number | null>(null);
  const [movieId, setMovieId] = useState("");
  const [showDate, setShowDate] = useState(() => todayIso());
  const [startTime, setStartTime] = useState("18:00");
  const [prices, setPrices] = useState<Partial<Record<SeatCategory, string>>>({});
  const [flags, setFlags] = useState<Record<FlagKey, boolean>>({
    quiet: false,
    sensoryFriendly: false,
    subtitles: false,
    lastMinuteDeals: true,
  });
  const movies = useAllMovies();
  const layout = useLayout(screenId);
  const mutation = useAddShow();

  const categories = useMemo(() => {
    const found = new Set<SeatCategory>();
    layout.data?.rows.forEach((row) => row.seats.forEach((seat) => found.add(seat.category)));
    return [...found];
  }, [layout.data]);

  const pricesOk = categories.length > 0 && categories.every((c) => Number(prices[c]) >= 0 && prices[c] !== undefined && prices[c] !== "");
  const canNext = [screenId !== null, movieId !== "" && showDate !== "" && startTime !== "", pricesOk];

  function save() {
    if (screenId === null) return;
    const priced: Partial<Record<SeatCategory, number>> = {};
    categories.forEach((c) => (priced[c] = Number(prices[c])));
    mutation.mutate({
      screenId,
      show: {
        movieId: Number(movieId),
        showDate,
        startTime: startTime.length === 5 ? `${startTime}:00` : startTime,
        prices: priced,
        ...flags,
      },
    });
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <ol className="flex items-center gap-2" aria-label="Steps">
        {STEPS.map((label, index) => (
          <li key={label} className="flex flex-1 items-center gap-2">
            <span
              className={cn(
                "flex size-8 shrink-0 items-center justify-center rounded-full text-sm font-semibold transition",
                index < step && "bg-emerald-500 text-white",
                index === step && "bg-primary text-primary-foreground scale-110 shadow",
                index > step && "bg-muted text-muted-foreground",
              )}
            >
              {index < step ? <CheckIcon className="size-4" /> : index + 1}
            </span>
            <span className="hidden text-sm sm:inline">{label}</span>
            {index < STEPS.length - 1 && <span className="h-0.5 flex-1 rounded bg-muted" />}
          </li>
        ))}
      </ol>
      <div className="h-1.5 overflow-hidden rounded-full bg-muted">
        <div
          className="h-full rounded-full bg-primary transition-all duration-500"
          style={{ width: `${((step + 1) / STEPS.length) * 100}%` }}
        />
      </div>

      <div key={step} className="animate-fade-up space-y-5 rounded-3xl border bg-card p-5">
        {step === 0 && (
          <ScreenSelect idPrefix="show" onChange={(next) => setScreenId(next)} />
        )}

        {step === 1 && (
          <>
            <NativeSelect
              id="show-movie"
              label="Movie"
              placeholder="Choose movie"
              value={movieId}
              options={(movies.data?.items ?? []).map((m) => ({ value: m.id, label: m.title }))}
              onChange={(event) => setMovieId(event.target.value)}
            />
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="space-y-1.5 text-sm font-medium">
                Date
                <input
                  type="date"
                  min={todayIso()}
                  value={showDate}
                  onChange={(event) => setShowDate(event.target.value)}
                  className="h-10 w-full rounded-lg border bg-background px-3 font-normal"
                />
              </label>
              <label className="space-y-1.5 text-sm font-medium">
                Start time
                <input
                  type="time"
                  value={startTime}
                  onChange={(event) => setStartTime(event.target.value)}
                  className="h-10 w-full rounded-lg border bg-background px-3 font-normal"
                />
              </label>
            </div>
          </>
        )}

        {step === 2 && (
          <>
            <div className="grid gap-4 sm:grid-cols-3">
              {categories.map((category) => (
                <label key={category} className="space-y-1.5 text-sm font-medium">
                  {CATEGORY_LABEL[category]} price
                  <div className="flex items-center rounded-lg border bg-background px-3">
                    <span className="text-muted-foreground">₹</span>
                    <input
                      type="number"
                      min={0}
                      value={prices[category] ?? ""}
                      onChange={(event) =>
                        setPrices((current) => ({ ...current, [category]: event.target.value }))
                      }
                      className="w-full bg-transparent px-2 py-2 font-normal outline-none"
                    />
                  </div>
                </label>
              ))}
            </div>
            {layout.isLoading && <p className="text-sm text-muted-foreground">Loading seat types...</p>}
            <ul className="grid gap-2 sm:grid-cols-2">
              {FLAGS.map((flag) => (
                <li key={flag.key}>
                  <button
                    type="button"
                    aria-pressed={flags[flag.key]}
                    onClick={() => setFlags((current) => ({ ...current, [flag.key]: !current[flag.key] }))}
                    className={cn(
                      "w-full rounded-xl border p-3 text-left transition hover:-translate-y-0.5",
                      flags[flag.key] ? "border-primary bg-primary/10" : "bg-background",
                    )}
                  >
                    <span className="block text-sm font-medium">{flag.label}</span>
                    <span className="block text-xs text-muted-foreground">{flag.hint}</span>
                  </button>
                </li>
              ))}
            </ul>
          </>
        )}
      </div>

      <ResultNote
        success={mutation.isSuccess ? "Show added. People can book it now." : null}
        error={mutation.error?.message}
      />

      <div className="flex justify-between">
        <Button variant="ghost" disabled={step === 0} onClick={() => setStep(step - 1)}>
          Back
        </Button>
        {step < STEPS.length - 1 ? (
          <Button disabled={!canNext[step]} onClick={() => setStep(step + 1)}>
            Next
          </Button>
        ) : (
          <Button disabled={!canNext[step] || mutation.isPending} onClick={save}>
            {mutation.isPending ? "Saving..." : "Add show"}
          </Button>
        )}
      </div>
    </div>
  );
}
