import { Hero } from "@/components/layout/hero";
import { MoodSearch } from "@/modules/discover/components/mood-search";
import { MoviesInCity } from "@/modules/movie/components/movies-in-city";

export default function HomePage() {
  return (
    <div className="space-y-10">
      <Hero />
      <MoodSearch />
      <section className="space-y-4">
        <h2 className="text-2xl font-bold tracking-tight">Now showing</h2>
        <MoviesInCity />
      </section>
    </div>
  );
}
