export type Show = {
  id: number;
  cinemaId: number;
  screenId: number;
  movieId: number | null;
  movieName: string | null;
  showDate: string;
  startTime: string;
  price: number;
  quiet: boolean;
  sensoryFriendly: boolean;
  subtitles: boolean;
};
