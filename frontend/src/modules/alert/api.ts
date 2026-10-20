import { appGet, appPost, type ListResponse } from "@/lib/api";
import type { CreateAlertInput, PriceAlert } from "@/modules/alert/types";

export const fetchMyAlerts = () => appGet<ListResponse<PriceAlert>>("/api/price-alerts");

export const createAlert = (input: CreateAlertInput) =>
  appPost<PriceAlert>("/api/price-alerts", input);

export const cancelAlert = (id: number) =>
  appPost<PriceAlert>(`/api/price-alerts/${id}/cancel`);
