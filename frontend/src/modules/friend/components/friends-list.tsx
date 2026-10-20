"use client";

import { UsersIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Avatar } from "@/modules/friend/components/avatar";
import { useFriends, useRemoveFriend } from "@/modules/friend/hooks";

export function FriendsList() {
  const { data, isLoading, error } = useFriends();
  const remove = useRemoveFriend();

  if (isLoading) return <Skeleton className="h-24 w-full rounded-2xl" />;
  if (error) return <p className="text-destructive">{error.message}</p>;
  if (!data || data.items.length === 0) {
    return (
      <div className="flex flex-col items-center gap-2 rounded-2xl border border-dashed py-12 text-center text-muted-foreground">
        <UsersIcon className="size-10" />
        <p>No friends yet. Add one by email.</p>
      </div>
    );
  }

  return (
    <ul className="space-y-2">
      {data.items.map((friend, index) => (
        <li
          key={friend.userId}
          className="animate-fade-up flex items-center justify-between gap-3 rounded-2xl border bg-card p-3"
          style={{ ["--delay" as string]: `${index * 50}ms` }}
        >
          <span className="flex items-center gap-3">
            <Avatar name={friend.name} userId={friend.userId} />
            <span>
              <span className="block font-medium">{friend.name}</span>
              <span className="block text-xs text-muted-foreground">
                Friends since{" "}
                {new Date(friend.since).toLocaleDateString("en-IN", {
                  day: "numeric",
                  month: "short",
                  year: "numeric",
                })}
              </span>
            </span>
          </span>
          <Button
            size="sm"
            variant="ghost"
            disabled={remove.isPending}
            onClick={() => {
              if (window.confirm(`Remove ${friend.name} from your friends?`)) {
                remove.mutate(friend.userId);
              }
            }}
          >
            Remove
          </Button>
        </li>
      ))}
    </ul>
  );
}
