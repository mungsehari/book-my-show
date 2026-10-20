"use client";

import { Button } from "@/components/ui/button";
import { Avatar } from "@/modules/friend/components/avatar";
import { useAnswerRequest, useFriendRequests } from "@/modules/friend/hooks";

export function RequestsList() {
  const { data } = useFriendRequests();
  const answer = useAnswerRequest();

  if (!data || data.items.length === 0) return null;

  return (
    <section className="space-y-3">
      <h2 className="text-lg font-semibold">
        Friend requests <span className="text-muted-foreground">({data.items.length})</span>
      </h2>
      {answer.error && <p className="text-sm text-destructive">{answer.error.message}</p>}
      <ul className="space-y-2">
        {data.items.map((request) => (
          <li
            key={request.id}
            className="animate-fade-up flex items-center justify-between gap-3 rounded-2xl border bg-card p-3"
          >
            <span className="flex items-center gap-3">
              <Avatar name={request.fromName} userId={request.fromUserId} />
              <span className="font-medium">{request.fromName}</span>
            </span>
            <span className="flex gap-2">
              <Button
                size="sm"
                disabled={answer.isPending}
                onClick={() => answer.mutate({ id: request.id, action: "accept" })}
              >
                Accept
              </Button>
              <Button
                size="sm"
                variant="outline"
                disabled={answer.isPending}
                onClick={() => answer.mutate({ id: request.id, action: "decline" })}
              >
                Decline
              </Button>
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}
