import { useQuery } from "@tanstack/react-query";
import { fetchCities } from "@/modules/city/api";

export const useCities = () =>
  useQuery({ queryKey: ["cities"], queryFn: fetchCities, staleTime: 5 * 60_000 });
