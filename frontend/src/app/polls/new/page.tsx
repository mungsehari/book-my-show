import { Suspense } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { CreatePollForm } from "@/modules/poll/components/create-poll-form";

export default function NewPollPage() {
  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">New poll</h1>
      {/* The form reads today's date, so it renders when the page is opened. */}
      <Suspense fallback={<Skeleton className="h-96 w-full rounded-3xl" />}>
        <CreatePollForm />
      </Suspense>
    </div>
  );
}
