"use client";

import { PlusIcon, Trash2Icon } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { CinemaSelect } from "@/modules/admin/components/cinema-select";
import { NativeSelect } from "@/modules/admin/components/native-select";
import { ResultNote } from "@/modules/admin/components/result-note";
import { ScreenPreview } from "@/modules/admin/components/screen-preview";
import { useAddScreen } from "@/modules/admin/hooks";
import { CATEGORIES, CATEGORY_LABEL, type RowInput, type SeatCategory } from "@/modules/admin/types";

const MAX_ROWS = 26;
const LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

/** Parses "1, 2, 15" into seat numbers. Ignores anything else. */
function parseSeats(text: string): number[] {
  return text
    .split(",")
    .map((part) => Number(part.trim()))
    .filter((n) => Number.isInteger(n) && n > 0);
}

const START_ROWS: RowInput[] = [
  { label: "A", seatCount: 10, category: "REGULAR" },
  { label: "B", seatCount: 10, category: "REGULAR" },
  { label: "C", seatCount: 10, category: "PREMIUM" },
];

export function ScreenBuilder() {
  const [cinemaId, setCinemaId] = useState<number | null>(null);
  const [name, setName] = useState("");
  const [rows, setRows] = useState<RowInput[]>(START_ROWS);
  const [wheelchairText, setWheelchairText] = useState<Record<string, string>>({});
  const mutation = useAddScreen();

  const update = (label: string, patch: Partial<RowInput>) =>
    setRows((current) => current.map((row) => (row.label === label ? { ...row, ...patch } : row)));

  const addRow = () =>
    setRows((current) => {
      if (current.length >= MAX_ROWS) return current;
      const last = current[current.length - 1];
      return [
        ...current,
        { label: LETTERS[current.length], seatCount: last?.seatCount ?? 10, category: last?.category ?? "REGULAR" },
      ];
    });

  // Keeping letters in order A, B, C... after a removal.
  const removeRow = (label: string) =>
    setRows((current) =>
      current.filter((row) => row.label !== label).map((row, index) => ({ ...row, label: LETTERS[index] })),
    );

  const ready = cinemaId !== null && name.trim() !== "" && rows.length > 0;

  function save() {
    if (cinemaId === null) return;
    const payload = rows.map((row) => ({
      ...row,
      wheelchairSeats: parseSeats(wheelchairText[row.label] ?? ""),
    }));
    mutation.mutate({ cinemaId, name: name.trim(), rows: payload });
  }

  return (
    <div className="grid gap-8 xl:grid-cols-[1fr_1fr]">
      <div className="space-y-5">
        <CinemaSelect idPrefix="screen" onChange={setCinemaId} />
        <div className="space-y-1.5">
          <label htmlFor="screen-name" className="text-sm font-medium">
            Screen name
          </label>
          <input
            id="screen-name"
            value={name}
            maxLength={50}
            onChange={(event) => setName(event.target.value)}
            placeholder="Screen 1"
            className="h-10 w-full rounded-lg border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
          />
        </div>

        <div className="space-y-3">
          <h2 className="font-semibold">Rows</h2>
          {rows.map((row) => (
            <div key={row.label} className="animate-pop-in flex flex-wrap items-end gap-3 rounded-xl border bg-card p-3">
              <span className="flex size-9 items-center justify-center rounded-lg bg-muted font-bold">
                {row.label}
              </span>
              <label className="space-y-1 text-xs">
                Seats
                <input
                  type="number"
                  min={1}
                  max={50}
                  value={row.seatCount}
                  onChange={(event) =>
                    update(row.label, { seatCount: Math.min(50, Math.max(1, Number(event.target.value) || 1)) })
                  }
                  className="block h-9 w-20 rounded-lg border bg-background px-2 text-sm"
                />
              </label>
              <div className="w-32">
                <NativeSelect
                  id={`cat-${row.label}`}
                  label="Type"
                  value={row.category}
                  options={CATEGORIES.map((c) => ({ value: c, label: CATEGORY_LABEL[c] }))}
                  onChange={(event) => update(row.label, { category: event.target.value as SeatCategory })}
                />
              </div>
              <label className="min-w-24 flex-1 space-y-1 text-xs">
                Wheelchair seat numbers
                <input
                  value={wheelchairText[row.label] ?? ""}
                  onChange={(event) =>
                    setWheelchairText((current) => ({ ...current, [row.label]: event.target.value }))
                  }
                  placeholder="1, 2"
                  className="block h-9 w-full rounded-lg border bg-background px-2 text-sm"
                />
              </label>
              <Button
                type="button"
                size="icon"
                variant="ghost"
                aria-label={`Remove row ${row.label}`}
                onClick={() => removeRow(row.label)}
              >
                <Trash2Icon />
              </Button>
            </div>
          ))}
          <Button type="button" variant="outline" onClick={addRow} disabled={rows.length >= MAX_ROWS}>
            <PlusIcon /> Add row
          </Button>
        </div>

        <ResultNote
          success={mutation.isSuccess ? "Screen saved." : null}
          error={mutation.error?.message}
        />
        <Button size="lg" disabled={!ready || mutation.isPending} onClick={save}>
          {mutation.isPending ? "Saving..." : "Save screen"}
        </Button>
      </div>

      <aside className="space-y-2 xl:sticky xl:top-24 xl:self-start">
        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Live preview</p>
        <ScreenPreview
          rows={rows.map((row) => ({ ...row, wheelchairSeats: parseSeats(wheelchairText[row.label] ?? "") }))}
        />
        <ul className="flex flex-wrap justify-center gap-3 text-xs text-muted-foreground">
          <li className="flex items-center gap-1"><span className="size-3 rounded bg-sky-500/80" /> Regular</li>
          <li className="flex items-center gap-1"><span className="size-3 rounded bg-violet-500/80" /> Premium</li>
          <li className="flex items-center gap-1"><span className="size-3 rounded bg-amber-500/80" /> Recliner</li>
          <li className="flex items-center gap-1"><span className="size-3 rounded bg-blue-600" /> Wheelchair</li>
        </ul>
      </aside>
    </div>
  );
}
