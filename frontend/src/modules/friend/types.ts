export type Friend = { userId: number; name: string; since: string };

export type FriendRequest = {
  id: number;
  fromUserId: number;
  fromName: string;
  /** PENDING, or ACCEPTED when the other person had already asked you. */
  status: string;
  createdAt: string;
};

export type FriendSummary = { userId: number; name: string };

export type FriendsActivity = {
  movieId: number;
  watchedCount: number;
  watched: FriendSummary[];
  goingCount: number;
  going: FriendSummary[];
};

export type Privacy = { shareActivity: boolean };
