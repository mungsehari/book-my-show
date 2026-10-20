import { MovieForm } from "@/modules/admin/components/movie-form";

export default function NewMoviePage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Add a movie</h1>
        <p className="text-sm text-muted-foreground">Only full admins can add to the catalog.</p>
      </div>
      <MovieForm />
    </div>
  );
}
