import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { cancelAlert, createAlert, fetchMyAlerts } from "@/modules/alert/api";

const KEY = ["alerts", "me"];

export const useMyAlerts = () => useQuery({ queryKey: KEY, queryFn: fetchMyAlerts });

export function useCreateAlert() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: createAlert,
    onSuccess: () => client.invalidateQueries({ queryKey: KEY }),
  });
}

export function useCancelAlert() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: cancelAlert,
    onSuccess: () => client.invalidateQueries({ queryKey: KEY }),
  });
}
