"use client";

import { useState } from "react";
import { NativeSelect } from "@/modules/admin/components/native-select";
import { useCinemasInCity } from "@/modules/cinema/hooks";
import { useCities } from "@/modules/city/hooks";

type Props = {
  idPrefix: string;
  onChange: (cinemaId: number | null) => void;
};

/** Pick a city, then a cinema in it. Tells the parent which cinema is chosen. */
export function CinemaSelect({ idPrefix, onChange }: Props) {
  const cities = useCities();
  const [cityId, setCityId] = useState<number | null>(null);
  const [cinemaId, setCinemaId] = useState<number | null>(null);
  const cinemas = useCinemasInCity(cityId);

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <NativeSelect
        id={`${idPrefix}-city`}
        label="City"
        placeholder="Choose city"
        value={cityId ?? ""}
        options={(cities.data?.items ?? []).map((city) => ({ value: city.id, label: city.name }))}
        onChange={(event) => {
          setCityId(event.target.value ? Number(event.target.value) : null);
          setCinemaId(null);
          onChange(null);
        }}
      />
      <NativeSelect
        id={`${idPrefix}-cinema`}
        label="Cinema"
        placeholder={cityId ? "Choose cinema" : "Choose a city first"}
        disabled={cityId === null}
        value={cinemaId ?? ""}
        options={(cinemas.data?.items ?? []).map((cinema) => ({ value: cinema.id, label: cinema.name }))}
        onChange={(event) => {
          const next = event.target.value ? Number(event.target.value) : null;
          setCinemaId(next);
          onChange(next);
        }}
      />
    </div>
  );
}
