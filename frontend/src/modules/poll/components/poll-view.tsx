"use client";

import { CheckCircle2Icon, CopyIcon, TrophyIcon } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { useSelectedCity } from "@/bootstrap/city-provider";
import { Button } from "@/components/ui/button";
import { formatTime } from "@/lib/format";
import { cn } from "@/lib/utils";
import { useMe } from "@/modules/auth/hooks";
import { useCinemasInCity } from "@/modules/cinema/hooks";
import { Avatar } from "@/modules/friend/components/avatar";
import { useClosePoll, useVote } from "@/modules/poll/hooks";
import type { Poll } from "@/modules/poll/types";

export function PollView({ poll }: { poll: Poll }) {
  const { data: user } = useMe();
  const { cityId } = useSelectedCity();
  const cinemas = useCinemasInCity(cityId);
  const vote = useVote();
  const close = useClosePoll();
  const [copied, setCopied] = useState(false);

  const open = poll.status === "OPEN";
  const isOwner = user?.id === poll.creatorId;
  const totalVotes = poll.options.reduce((sum, option) => sum + option.votes, 0);

  async function copyLink() {
    try {
      await navigator.clipboard.writeText(`${window.location.origin}/polls/${poll.id}`);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard can be blocked. The address bar link still works.
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <header className="space-y-1">
        <div className="flex items-start justify-between gap-3">
          <h1 className="text-2xl font-extrabold tracking-tight">{poll.title}</h1>
          <span
            className={cn(
              "rounded-full px-3 py-1 text-xs font-medium",
              open ? "bg-emerald-500/15 text-emerald-600" : "bg-muted text-muted-foreground",
            )}
          >
            {open ? "Voting open" : "Closed"}
          </span>
        </div>
        <p className="text-sm text-muted-foreground">
          {totalVotes} of {poll.members.length} voted
        </p>
      </header>

      <ul className="space-y-3">
        {poll.options.map((option) => {
          const cinema = cinemas.data?.items.find((item) => item.id === option.cinemaId);
          const percent = totalVotes ? Math.round((option.votes / totalVotes) * 100) : 0;
          const mine = poll.myVote === option.showId;
          const winner = poll.winningShowId === option.showId;
          return (
            <li
              key={option.showId}
              className={cn(
                "relative overflow-hidden rounded-2xl border bg-card p-4",
                (mine || winner) && "border-primary",
              )}
            >
              <div
                className="absolute inset-y-0 left-0 bg-primary/10 transition-all duration-700"
                style={{ width: `${percent}%` }}
                aria-hidden
              />
              <div className="relative flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="flex items-center gap-2 font-semibold">
                    {option.movieName}
                    {winner && <TrophyIcon className="size-4 text-amber-500" aria-label="Winner" />}
                    {mine && <CheckCircle2Icon className="size-4 text-primary" aria-label="Your vote" />}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {cinema?.name ?? `Cinema #${option.cinemaId}`} ·{" "}
                    {new Date(`${option.showDate}T00:00:00`).toLocaleDateString("en-IN", {
                      weekday: "short",
                      day: "numeric",
                      month: "short",
                    })}{" "}
                    · {formatTime(option.startTime)}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-sm font-semibold tabular-nums">
                    {option.votes} · {percent}%
                  </span>
                  {open && (
                    <Button
                      size="sm"
                      variant={mine ? "default" : "outline"}
                      disabled={vote.isPending}
                      onClick={() => vote.mutate({ id: poll.id, showId: option.showId })}
                    >
                      {mine ? "Voted" : "Vote"}
                    </Button>
                  )}
                  {winner && (
                    <Button size="sm" asChild>
                      <Link href={`/shows/${option.showId}/seats`}>Book now</Link>
                    </Button>
                  )}
                </div>
              </div>
            </li>
          );
        })}
      </ul>

      {(vote.error || close.error) && (
        <p className="text-sm text-destructive">{(vote.error ?? close.error)?.message}</p>
      )}

      <section className="space-y-2">
        <h2 className="text-sm font-semibold">Who is in</h2>
        <ul className="flex flex-wrap gap-3">
          {poll.members.map((member) => (
            <li key={member.userId} className="flex items-center gap-2 text-sm">
              <Avatar name={member.name} userId={member.userId} />
              <span>{member.name}</span>
              <span className={member.voted ? "text-emerald-600" : "text-muted-foreground"}>
                {member.voted ? "voted" : "waiting"}
              </span>
            </li>
          ))}
        </ul>
      </section>

      <div className="flex flex-wrap gap-2">
        <Button variant="outline" onClick={copyLink}>
          <CopyIcon /> {copied ? "Link copied" : "Copy link"}
        </Button>
        {open && isOwner && (
          <Button
            variant="outline"
            disabled={close.isPending}
            onClick={() => {
              if (window.confirm("Close voting and pick the winner?")) close.mutate(poll.id);
            }}
          >
            {close.isPending ? "Closing..." : "Close voting"}
          </Button>
        )}
      </div>
    </div>
  );
}
