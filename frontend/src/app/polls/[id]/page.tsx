import { Suspense } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { PollDetail } from "@/modules/poll/components/poll-detail";

async function PollFromParams({ params }: PageProps<"/polls/[id]">) {
  const { id } = await params;
  return <PollDetail pollId={Number(id)} />;
}

export default function PollPage(props: PageProps<"/polls/[id]">) {
  return (
    <Suspense fallback={<Skeleton className="mx-auto h-72 w-full max-w-2xl rounded-3xl" />}>
      <PollFromParams {...props} />
    </Suspense>
  );
}
