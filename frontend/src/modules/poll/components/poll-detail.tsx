"use client";

import { Skeleton } from "@/components/ui/skeleton";
import { PollView } from "@/modules/poll/components/poll-view";
import { usePoll } from "@/modules/poll/hooks";

export function PollDetail({ pollId }: { pollId: number }) {
  const { data, isLoading, error } = usePoll(pollId);

  if (isLoading) return <Skeleton className="mx-auto h-72 w-full max-w-2xl rounded-3xl" />;
  if (error || !data) {
    return <p className="text-center text-destructive">{error?.message ?? "Poll not found."}</p>;
  }
  return <PollView poll={data} />;
}
