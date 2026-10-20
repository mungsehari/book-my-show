export type Movie = {
  id: number;
  title: string;
  description: string;
  language: string;
  genre: string;
  durationMinutes: number;
  certificate: string;
  releaseDate: string;
  posterUrl: string | null;
};
