import { useQuery } from "@tanstack/react-query";
import { fetchCinemasInCity } from "@/modules/cinema/api";

export const useCinemasInCity = (cityId: number | null) =>
  useQuery({
    queryKey: ["cinemas", "city", cityId],
    queryFn: () => fetchCinemasInCity(cityId as number),
    enabled: cityId !== null,
  });
