import { Suspense } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { ShowWizard } from "@/modules/admin/components/show-wizard";

export default function NewShowPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Add a show</h1>
        <p className="text-sm text-muted-foreground">
          Three quick steps. Shows on the same screen cannot overlap.
        </p>
      </div>
      {/* The wizard reads today's date, so it renders when the page is opened. */}
      <Suspense fallback={<Skeleton className="h-96 w-full rounded-3xl" />}>
        <ShowWizard />
      </Suspense>
    </div>
  );
}
