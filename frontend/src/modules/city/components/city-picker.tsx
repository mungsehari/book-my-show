"use client";

import { useSelectedCity } from "@/bootstrap/city-provider";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useCities } from "@/modules/city/hooks";

export function CityPicker() {
  const { cityId, setCityId } = useSelectedCity();
  const { data, isLoading } = useCities();

  return (
    <Select
      value={cityId ? String(cityId) : ""}
      onValueChange={(value) => setCityId(Number(value))}
      disabled={isLoading}
    >
      <SelectTrigger className="w-44" aria-label="Choose city">
        <SelectValue placeholder="Select city" />
      </SelectTrigger>
      <SelectContent>
        {data?.items.map((city) => (
          <SelectItem key={city.id} value={String(city.id)}>
            {city.name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
