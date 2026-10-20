"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { CinemaSelect } from "@/modules/admin/components/cinema-select";
import { NativeSelect } from "@/modules/admin/components/native-select";
import { ResultNote } from "@/modules/admin/components/result-note";
import { ScreenSelect } from "@/modules/admin/components/screen-select";
import { useAddPlace, useAddView } from "@/modules/admin/hooks";

const INPUT =
  "h-10 w-full rounded-lg border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring";

function Panel({ title, hint, children }: { title: string; hint: string; children: React.ReactNode }) {
  return (
    <section className="animate-fade-up space-y-4 rounded-3xl border bg-card p-5">
      <div>
        <h2 className="font-semibold">{title}</h2>
        <p className="text-sm text-muted-foreground">{hint}</p>
      </div>
      {children}
    </section>
  );
}

export function PlaceForm() {
  const [cinemaId, setCinemaId] = useState<number | null>(null);
  const [type, setType] = useState<"FOOD" | "PARKING">("FOOD");
  const [name, setName] = useState("");
  const [walk, setWalk] = useState("5");
  const [notes, setNotes] = useState("");
  const mutation = useAddPlace();

  return (
    <Panel title="Food and parking" hint="Shown in the weekend planner next to your cinema.">
      <CinemaSelect idPrefix="place" onChange={setCinemaId} />
      <div className="grid gap-4 sm:grid-cols-2">
        <NativeSelect
          id="place-type"
          label="Type"
          value={type}
          options={[
            { value: "FOOD", label: "Food" },
            { value: "PARKING", label: "Parking" },
          ]}
          onChange={(event) => setType(event.target.value as "FOOD" | "PARKING")}
        />
        <label className="space-y-1.5 text-sm font-medium">
          Walk (minutes, 0 to 60)
          <input type="number" min={0} max={60} value={walk} onChange={(e) => setWalk(e.target.value)} className={INPUT} />
        </label>
      </div>
      <label className="block space-y-1.5 text-sm font-medium">
        Name
        <input value={name} maxLength={150} onChange={(e) => setName(e.target.value)} className={INPUT} />
      </label>
      <label className="block space-y-1.5 text-sm font-medium">
        Notes (optional)
        <input value={notes} maxLength={300} onChange={(e) => setNotes(e.target.value)} className={INPUT} />
      </label>
      <ResultNote success={mutation.isSuccess ? "Place added." : null} error={mutation.error?.message} />
      <Button
        disabled={cinemaId === null || !name.trim() || mutation.isPending}
        onClick={() =>
          cinemaId !== null &&
          mutation.mutate({
            cinemaId,
            place: { type, name: name.trim(), walkMinutes: Number(walk), notes: notes.trim() || undefined },
          })
        }
      >
        {mutation.isPending ? "Saving..." : "Add place"}
      </Button>
    </Panel>
  );
}

export function ViewForm() {
  const [screenId, setScreenId] = useState<number | null>(null);
  const [fromRow, setFromRow] = useState("A");
  const [toRow, setToRow] = useState("C");
  const [imageUrl, setImageUrl] = useState("");
  const [caption, setCaption] = useState("");
  const mutation = useAddView();
  const validUrl = /^https:\/\/\S+$/.test(imageUrl);
  const validRow = (value: string) => /^[A-Z]$/.test(value);

  return (
    <Panel title="Seat view photo" hint="Show what the screen looks like from a block of rows.">
      <ScreenSelect idPrefix="view" onChange={(next) => setScreenId(next)} />
      <div className="grid gap-4 sm:grid-cols-2">
        <label className="space-y-1.5 text-sm font-medium">
          From row
          <input value={fromRow} maxLength={1} onChange={(e) => setFromRow(e.target.value.toUpperCase())} className={INPUT} />
        </label>
        <label className="space-y-1.5 text-sm font-medium">
          To row
          <input value={toRow} maxLength={1} onChange={(e) => setToRow(e.target.value.toUpperCase())} className={INPUT} />
        </label>
      </div>
      <label className="block space-y-1.5 text-sm font-medium">
        Photo link (https)
        <input value={imageUrl} onChange={(e) => setImageUrl(e.target.value)} placeholder="https://..." className={INPUT} />
      </label>
      <label className="block space-y-1.5 text-sm font-medium">
        Caption (optional)
        <input value={caption} maxLength={200} onChange={(e) => setCaption(e.target.value)} className={INPUT} />
      </label>
      {validUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={imageUrl} alt="Seat view preview" className="animate-pop-in max-h-48 w-full rounded-xl object-cover" />
      )}
      <ResultNote success={mutation.isSuccess ? "Photo added." : null} error={mutation.error?.message} />
      <Button
        disabled={screenId === null || !validUrl || !validRow(fromRow) || !validRow(toRow) || mutation.isPending}
        onClick={() =>
          screenId !== null &&
          mutation.mutate({
            screenId,
            view: { fromRow, toRow, imageUrl, caption: caption.trim() || undefined },
          })
        }
      >
        {mutation.isPending ? "Saving..." : "Add photo"}
      </Button>
    </Panel>
  );
}
