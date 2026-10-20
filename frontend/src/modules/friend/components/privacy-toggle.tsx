"use client";

import { cn } from "@/lib/utils";
import { usePrivacy, useSavePrivacy } from "@/modules/friend/hooks";

export function PrivacyToggle() {
  const { data } = usePrivacy();
  const save = useSavePrivacy();
  if (!data) return null;

  const on = data.shareActivity;
  return (
    <div className="flex items-center justify-between gap-4 rounded-2xl border bg-card p-4">
      <div>
        <p className="font-medium">Share my movie activity with friends</p>
        <p className="text-sm text-muted-foreground">
          Friends can see which movies you watched and which shows you are going to.
        </p>
      </div>
      <button
        type="button"
        role="switch"
        aria-checked={on}
        aria-label="Share my movie activity"
        disabled={save.isPending}
        onClick={() => save.mutate(!on)}
        className={cn(
          "relative h-7 w-12 shrink-0 rounded-full transition",
          on ? "bg-primary" : "bg-muted-foreground/30",
        )}
      >
        <span
          className={cn(
            "absolute top-0.5 size-6 rounded-full bg-white shadow transition-all",
            on ? "left-[1.4rem]" : "left-0.5",
          )}
        />
      </button>
    </div>
  );
}
