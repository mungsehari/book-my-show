import { apiGet, appPost } from "@/lib/api";
import type { SeatLock, SeatMap } from "@/modules/seat/types";

export const fetchSeatMap = (showId: number) =>
  apiGet<SeatMap>(`/shows/${showId}/seats`);

export const lockSeats = (showId: number, seatIds: number[]) =>
  appPost<SeatLock>(`/api/shows/${showId}/locks`, { seatIds });

export const releaseLock = (lockId: string) =>
  appPost<void>(`/api/locks/${lockId}/release`);
