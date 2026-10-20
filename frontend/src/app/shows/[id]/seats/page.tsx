import { Suspense } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { SeatPicker } from "@/modules/seat/components/seat-picker";

async function SeatsFromParams({ params }: PageProps<"/shows/[id]/seats">) {
  const { id } = await params;
  return <SeatPicker showId={Number(id)} />;
}

export default function SeatsPage(props: PageProps<"/shows/[id]/seats">) {
  return (
    <Suspense fallback={<Skeleton className="h-96 w-full rounded-3xl" />}>
      <SeatsFromParams {...props} />
    </Suspense>
  );
}
