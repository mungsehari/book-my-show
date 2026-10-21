import { Suspense } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { AdminOverview } from "@/modules/admin/components/admin-overview";

export default function AdminPage() {
  return (
    <>
      {/* The dashboard reads today's date, so it renders when the page is opened. */}
      <Suspense fallback={<Skeleton className="h-96 w-full rounded-3xl" />}>
        <AdminOverview />
      </Suspense>
    </>
  );
}
