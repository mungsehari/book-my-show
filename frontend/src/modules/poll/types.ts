export type PollOption = {
  showId: number;
  movieName: string | null;
  cinemaId: number;
  showDate: string;
  startTime: string;
  votes: number;
};

export type PollMember = { userId: number; name: string; voted: boolean };

export type Poll = {
  id: number;
  title: string;
  /** OPEN or CLOSED. */
  status: string;
  creatorId: number;
  options: PollOption[];
  members: PollMember[];
  myVote: number | null;
  winningShowId: number | null;
};

export type CreatePollInput = {
  title: string;
  showIds: number[];
  friendIds: number[];
};

export const MIN_POLL_SHOWS = 2;
export const MAX_POLL_SHOWS = 5;
export const MAX_POLL_FRIENDS = 20;
