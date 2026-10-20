import { Suspense } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { BookingDetail } from "@/modules/booking/components/booking-detail";

async function BookingFromParams({ params, searchParams }: PageProps<"/bookings/[id]">) {
  const { id } = await params;
  const { paid } = await searchParams;
  return <BookingDetail bookingId={Number(id)} justPaid={paid === "1"} />;
}

export default function BookingPage(props: PageProps<"/bookings/[id]">) {
  return (
    <Suspense fallback={<Skeleton className="h-80 w-full rounded-3xl" />}>
      <BookingFromParams {...props} />
    </Suspense>
  );
}
