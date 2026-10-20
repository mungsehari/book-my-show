"use client";

import { CheckIcon, XIcon } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { formatTime } from "@/lib/format";
import { useCheckIn } from "@/modules/admin/hooks";

export function ScanTicket() {
  const [code, setCode] = useState("");
  const mutation = useCheckIn();

  return (
    <div className="mx-auto max-w-md space-y-6">
      <form
        className="space-y-3"
        onSubmit={(event) => {
          event.preventDefault();
          mutation.mutate(code.trim(), { onSuccess: () => setCode("") });
        }}
      >
        <label htmlFor="ticket-code" className="text-sm font-medium">
          Ticket code (from the QR)
        </label>
        <input
          id="ticket-code"
          value={code}
          autoFocus
          autoComplete="off"
          onChange={(event) => {
            setCode(event.target.value);
            if (mutation.data || mutation.error) mutation.reset();
          }}
          placeholder="Paste or scan the code"
          className="h-12 w-full rounded-xl border bg-background px-4 font-mono text-sm outline-none focus:ring-2 focus:ring-ring"
        />
        <Button type="submit" size="lg" className="w-full" disabled={!code.trim() || mutation.isPending}>
          {mutation.isPending ? "Checking..." : "Check in"}
        </Button>
      </form>

      {mutation.data && (
        <div className="animate-pop-in space-y-3 rounded-3xl border border-emerald-500/50 bg-emerald-500/10 p-6 text-center">
          <span className="mx-auto flex size-16 items-center justify-center rounded-full bg-emerald-500 text-white shadow-lg">
            <CheckIcon className="size-9" />
          </span>
          <h2 className="text-xl font-extrabold text-emerald-700 dark:text-emerald-300">Entry allowed</h2>
          <p className="font-semibold">{mutation.data.movieName}</p>
          <p className="text-sm text-muted-foreground">
            {mutation.data.cinemaName} · {mutation.data.screenName} · {formatTime(mutation.data.startTime)}
          </p>
          <p className="text-lg font-bold">{mutation.data.seats.join(", ")}</p>
        </div>
      )}

      {mutation.error && (
        <div className="animate-pop-in space-y-2 rounded-3xl border border-destructive/50 bg-destructive/10 p-6 text-center">
          <span className="mx-auto flex size-16 items-center justify-center rounded-full bg-destructive text-white shadow-lg">
            <XIcon className="size-9" />
          </span>
          <h2 className="text-xl font-extrabold text-destructive">Do not allow entry</h2>
          <p className="text-sm">{mutation.error.message}</p>
        </div>
      )}
    </div>
  );
}
