import { Suspense } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { MovieDetail } from "@/modules/movie/components/movie-detail";

async function MovieFromParams({ params }: PageProps<"/movies/[id]">) {
  const { id } = await params;
  return <MovieDetail movieId={Number(id)} />;
}

export default function MoviePage(props: PageProps<"/movies/[id]">) {
  return (
    <Suspense fallback={<Skeleton className="h-64 w-full" />}>
      <MovieFromParams {...props} />
    </Suspense>
  );
}
