"use client";

import { UsersRoundIcon } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { formatPrice } from "@/lib/format";
import { Avatar } from "@/modules/friend/components/avatar";
import { useFriends } from "@/modules/friend/hooks";
import { useCreateSplit } from "@/modules/booking/hooks";

const MAX_FRIENDS = 9;

type Props = { lockId: string; total: number };

/** "Split with friends": each friend pays an equal share within 15 minutes. */
export function SplitSection({ lockId, total }: Props) {
  const router = useRouter();
  const friends = useFriends();
  const split = useCreateSplit();
  const [open, setOpen] = useState(false);
  const [chosen, setChosen] = useState<number[]>([]);

  const toggle = (id: number) =>
    setChosen((current) =>
      current.includes(id)
        ? current.filter((item) => item !== id)
        : current.length < MAX_FRIENDS
          ? [...current, id]
          : current,
    );

  if (!open) {
    return (
      <Button variant="outline" className="mx-auto flex" onClick={() => setOpen(true)}>
        <UsersRoundIcon /> Split with friends
      </Button>
    );
  }

  const perPerson = total / (chosen.length + 1);

  return (
    <div className="animate-fade-up mx-auto max-w-md space-y-4 rounded-3xl border bg-card p-5 shadow-lg">
      <div>
        <h2 className="font-bold">Split the bill</h2>
        <p className="text-sm text-muted-foreground">
          Each friend gets a share to pay within 15 minutes. Tickets are issued when everyone
          has paid.
        </p>
      </div>

      {friends.data && friends.data.items.length === 0 && (
        <p className="text-sm text-muted-foreground">
          You have no friends yet.{" "}
          <Link href="/friends" className="text-primary underline">
            Add some first
          </Link>
          .
        </p>
      )}
      <ul className="grid gap-2 sm:grid-cols-2">
        {friends.data?.items.map((friend) => {
          const on = chosen.includes(friend.userId);
          return (
            <li key={friend.userId}>
              <button
                type="button"
                aria-pressed={on}
                onClick={() => toggle(friend.userId)}
                className={`flex w-full items-center gap-2 rounded-xl border p-2 text-left text-sm transition ${
                  on ? "border-primary bg-primary/10" : "bg-background"
                }`}
              >
                <Avatar name={friend.name} userId={friend.userId} />
                {friend.name}
              </button>
            </li>
          );
        })}
      </ul>

      {chosen.length > 0 && (
        <p className="rounded-xl bg-muted p-3 text-center text-sm">
          {chosen.length + 1} people · about <strong>{formatPrice(perPerson)}</strong> each
        </p>
      )}
      {split.error && <p className="text-sm text-destructive">{split.error.message}</p>}
      <div className="flex gap-2">
        <Button
          disabled={chosen.length === 0 || split.isPending}
          onClick={() =>
            split.mutate(
              { lockId, friendIds: chosen },
              { onSuccess: (result) => router.push(`/bookings/${result.bookingId}`) },
            )
          }
        >
          {split.isPending ? "Creating..." : "Send payment requests"}
        </Button>
        <Button variant="ghost" onClick={() => setOpen(false)}>
          Cancel
        </Button>
      </div>
    </div>
  );
}
