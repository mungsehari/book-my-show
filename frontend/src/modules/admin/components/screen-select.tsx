"use client";

import { useState } from "react";
import { CinemaSelect } from "@/modules/admin/components/cinema-select";
import { NativeSelect } from "@/modules/admin/components/native-select";
import { useScreens } from "@/modules/admin/hooks";

type Props = {
  idPrefix: string;
  onChange: (screenId: number | null, cinemaId: number | null) => void;
};

/** Pick city, cinema, then one of its screens. */
export function ScreenSelect({ idPrefix, onChange }: Props) {
  const [cinemaId, setCinemaId] = useState<number | null>(null);
  const [screenId, setScreenId] = useState<number | null>(null);
  const screens = useScreens(cinemaId);

  return (
    <div className="space-y-4">
      <CinemaSelect
        idPrefix={idPrefix}
        onChange={(next) => {
          setCinemaId(next);
          setScreenId(null);
          onChange(null, next);
        }}
      />
      <NativeSelect
        id={`${idPrefix}-screen`}
        label="Screen"
        placeholder={cinemaId ? "Choose screen" : "Choose a cinema first"}
        disabled={cinemaId === null}
        value={screenId ?? ""}
        options={(screens.data?.items ?? []).map((screen) => ({
          value: screen.id,
          label: `${screen.name} (${screen.seatCount} seats)`,
        }))}
        onChange={(event) => {
          const next = event.target.value ? Number(event.target.value) : null;
          setScreenId(next);
          onChange(next, cinemaId);
        }}
      />
      {cinemaId !== null && screens.data?.items.length === 0 && (
        <p className="text-sm text-muted-foreground">This cinema has no screens yet. Add one first.</p>
      )}
    </div>
  );
}
