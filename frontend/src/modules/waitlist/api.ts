import { ApiError, appGet, appPost } from "@/lib/api";
import type { WaitlistEntry } from "@/modules/waitlist/types";

/** Your entry for a show, or null when you are not on the list. */
export async function fetchMyWaitlist(showId: number): Promise<WaitlistEntry | null> {
  try {
    return await appGet<WaitlistEntry>(`/api/shows/${showId}/waitlist`);
  } catch (error) {
    if (error instanceof ApiError && [401, 404].includes(error.status)) return null;
    throw error;
  }
}

export const joinWaitlist = (showId: number, seats: number) =>
  appPost<WaitlistEntry>(`/api/shows/${showId}/waitlist`, { seats });

export const leaveWaitlist = (entryId: number) =>
  appPost<void>(`/api/waitlist/${entryId}/leave`);
