"use client";

import { VoteIcon } from "lucide-react";
import Link from "next/link";
import { Skeleton } from "@/components/ui/skeleton";
import { useMyPolls } from "@/modules/poll/hooks";

export function PollsList() {
  const { data, isLoading, error } = useMyPolls();

  if (isLoading) return <Skeleton className="h-24 w-full rounded-2xl" />;
  if (error) return <p className="text-destructive">{error.message}</p>;
  if (!data || data.items.length === 0) {
    return (
      <div className="flex flex-col items-center gap-2 rounded-2xl border border-dashed py-14 text-center text-muted-foreground">
        <VoteIcon className="size-10" />
        <p>No polls yet. Start one and let your friends vote.</p>
      </div>
    );
  }

  return (
    <ul className="space-y-3">
      {data.items.map((poll, index) => (
        <li
          key={poll.id}
          className="animate-fade-up"
          style={{ ["--delay" as string]: `${index * 50}ms` }}
        >
          <Link
            href={`/polls/${poll.id}`}
            className="flex items-center justify-between gap-3 rounded-2xl border bg-card p-4 transition hover:-translate-y-0.5 hover:shadow-md"
          >
            <span>
              <span className="block font-semibold">{poll.title}</span>
              <span className="block text-xs text-muted-foreground">
                {poll.options.length} shows · {poll.members.filter((m) => m.voted).length} of{" "}
                {poll.members.length} voted
              </span>
            </span>
            <span className="text-xs text-muted-foreground">
              {poll.status === "OPEN" ? "Open" : "Closed"}
            </span>
          </Link>
        </li>
      ))}
    </ul>
  );
}
