import { Suspense } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { PlannerForm } from "@/modules/planner/components/planner-form";

export default function PlannerPage() {
  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Weekend planner</h1>
        <p className="text-sm text-muted-foreground">
          Movie, food and parking in one plan. Tell us when and how many.
        </p>
      </div>
      {/* The form reads today's date, so it renders when the page is opened. */}
      <Suspense fallback={<Skeleton className="h-96 w-full rounded-3xl" />}>
        <PlannerForm />
      </Suspense>
    </div>
  );
}
