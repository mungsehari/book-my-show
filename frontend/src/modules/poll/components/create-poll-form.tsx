"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Avatar } from "@/modules/friend/components/avatar";
import { useFriends } from "@/modules/friend/hooks";
import { ShowPicker, type PickedShow } from "@/modules/poll/components/show-picker";
import { useCreatePoll } from "@/modules/poll/hooks";
import { MAX_POLL_FRIENDS, MIN_POLL_SHOWS } from "@/modules/poll/types";

export function CreatePollForm() {
  const router = useRouter();
  const friends = useFriends();
  const mutation = useCreatePoll();
  const [title, setTitle] = useState("");
  const [picked, setPicked] = useState<PickedShow[]>([]);
  const [friendIds, setFriendIds] = useState<number[]>([]);

  const toggleShow = (show: PickedShow) =>
    setPicked((current) =>
      current.some((item) => item.id === show.id)
        ? current.filter((item) => item.id !== show.id)
        : [...current, show],
    );

  const toggleFriend = (id: number) =>
    setFriendIds((current) =>
      current.includes(id)
        ? current.filter((item) => item !== id)
        : current.length < MAX_POLL_FRIENDS
          ? [...current, id]
          : current,
    );

  const valid = title.trim().length > 0 && picked.length >= MIN_POLL_SHOWS;

  return (
    <form
      className="space-y-8"
      onSubmit={(event) => {
        event.preventDefault();
        mutation.mutate(
          { title: title.trim(), showIds: picked.map((show) => show.id), friendIds },
          { onSuccess: (poll) => router.push(`/polls/${poll.id}`) },
        );
      }}
    >
      <div className="space-y-2">
        <label htmlFor="poll-title" className="text-sm font-medium">
          What are you deciding?
        </label>
        <input
          id="poll-title"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          maxLength={100}
          placeholder="Saturday movie night"
          className="h-11 w-full rounded-lg border bg-background px-3 outline-none focus:ring-2 focus:ring-ring"
        />
      </div>

      <section className="space-y-3">
        <h2 className="font-semibold">
          Pick {MIN_POLL_SHOWS} to 5 shows{" "}
          <span className="text-sm font-normal text-muted-foreground">({picked.length} chosen)</span>
        </h2>
        {picked.length > 0 && (
          <ul className="flex flex-wrap gap-2">
            {picked.map((show) => (
              <li
                key={show.id}
                className="animate-pop-in rounded-full bg-primary/10 px-3 py-1 text-xs text-primary"
              >
                {show.label}
              </li>
            ))}
          </ul>
        )}
        <ShowPicker picked={picked} onToggle={toggleShow} />
      </section>

      <section className="space-y-3">
        <h2 className="font-semibold">Invite friends</h2>
        {friends.data && friends.data.items.length === 0 && (
          <p className="text-sm text-muted-foreground">
            You have no friends yet.{" "}
            <Link href="/friends" className="text-primary underline">
              Add some
            </Link>
            , or make the poll just for you.
          </p>
        )}
        <ul className="grid gap-2 sm:grid-cols-2">
          {friends.data?.items.map((friend) => {
            const on = friendIds.includes(friend.userId);
            return (
              <li key={friend.userId}>
                <button
                  type="button"
                  aria-pressed={on}
                  onClick={() => toggleFriend(friend.userId)}
                  className={`flex w-full items-center gap-3 rounded-xl border p-2 text-left transition ${
                    on ? "border-primary bg-primary/10" : "bg-card"
                  }`}
                >
                  <Avatar name={friend.name} userId={friend.userId} />
                  <span className="font-medium">{friend.name}</span>
                </button>
              </li>
            );
          })}
        </ul>
      </section>

      {mutation.error && <p className="text-sm text-destructive">{mutation.error.message}</p>}
      <Button type="submit" size="lg" disabled={!valid || mutation.isPending}>
        {mutation.isPending ? "Creating..." : "Create poll"}
      </Button>
    </form>
  );
}
