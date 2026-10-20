"use client";

import { EyeIcon, TicketIcon } from "lucide-react";
import { Avatar } from "@/modules/friend/components/avatar";
import { useMe } from "@/modules/auth/hooks";
import { useFriendsActivity } from "@/modules/friend/hooks";
import type { FriendSummary } from "@/modules/friend/types";

function Group({
  icon,
  text,
  people,
}: {
  icon: React.ReactNode;
  text: string;
  people: FriendSummary[];
}) {
  return (
    <div className="flex items-center gap-2 text-sm">
      {icon}
      <span className="flex -space-x-2">
        {people.slice(0, 4).map((person) => (
          <span key={person.userId} title={person.name} className="rounded-full ring-2 ring-card">
            <Avatar name={person.name} userId={person.userId} />
          </span>
        ))}
      </span>
      <span>{text}</span>
    </div>
  );
}

/** "3 friends watched this" for the movie page. Shows nothing when there is nothing to say. */
export function FriendsActivity({ movieId }: { movieId: number }) {
  const { data: user } = useMe();
  const { data } = useFriendsActivity(movieId, !!user);
  if (!data || (data.watchedCount === 0 && data.goingCount === 0)) return null;

  const plural = (count: number) => (count === 1 ? "friend" : "friends");
  return (
    <section className="animate-fade-up space-y-2 rounded-2xl border bg-card p-4">
      {data.watchedCount > 0 && (
        <Group
          icon={<EyeIcon className="size-4 text-primary" />}
          people={data.watched}
          text={`${data.watchedCount} ${plural(data.watchedCount)} watched this`}
        />
      )}
      {data.goingCount > 0 && (
        <Group
          icon={<TicketIcon className="size-4 text-primary" />}
          people={data.going}
          text={`${data.goingCount} ${plural(data.goingCount)} going soon`}
        />
      )}
    </section>
  );
}
