export type WaitlistEntry = {
  entryId: number;
  showId: number;
  seatsWanted: number;
  /** WAITING, OFFERED, or a closed state such as BOOKED, EXPIRED, LEFT. */
  status: string;
  position: number | null;
  lockId: string | null;
  offeredSeatIds: number[] | null;
  offerExpiresAt: string | null;
};
