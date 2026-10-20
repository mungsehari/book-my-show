import Link from "next/link";
import { Button } from "@/components/ui/button";
import { PollsList } from "@/modules/poll/components/polls-list";

export default function PollsPage() {
  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-bold tracking-tight">Group polls</h1>
        <Button asChild>
          <Link href="/polls/new">New poll</Link>
        </Button>
      </div>
      <PollsList />
    </div>
  );
}
