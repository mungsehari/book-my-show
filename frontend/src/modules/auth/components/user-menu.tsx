"use client";

import { BellIcon, CalendarRangeIcon, HandCoinsIcon, LayoutDashboardIcon, LogOutIcon, TicketIcon, UsersIcon, VoteIcon } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useLogout, useMe } from "@/modules/auth/hooks";

export function UserMenu() {
  const { data: user, isLoading } = useMe();
  const logout = useLogout();
  const router = useRouter();
  const [open, setOpen] = useState(false);

  if (isLoading) return <Skeleton className="h-9 w-20 rounded-lg" />;
  if (!user) {
    return (
      <Button asChild>
        <Link href="/login">Sign in</Link>
      </Button>
    );
  }

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-expanded={open}
        aria-label="Account menu"
        className="flex size-9 items-center justify-center rounded-full bg-primary font-semibold text-primary-foreground transition hover:scale-105"
      >
        {user.name.charAt(0).toUpperCase()}
      </button>
      {open && (
        <div className="animate-pop-in absolute right-0 mt-2 w-52 origin-top-right rounded-xl border bg-popover p-2 shadow-xl">
          <p className="truncate px-2 py-1 text-sm font-medium">{user.name}</p>
          <Link
            href="/bookings"
            onClick={() => setOpen(false)}
            className="flex items-center gap-2 rounded-lg px-2 py-2 text-sm hover:bg-muted"
          >
            <TicketIcon className="size-4" /> My bookings
          </Link>
          {(user.role === "ADMIN" || user.role === "THEATRE_ADMIN") && (
            <Link
              href="/admin"
              onClick={() => setOpen(false)}
              className="flex items-center gap-2 rounded-lg px-2 py-2 text-sm font-medium text-primary hover:bg-muted"
            >
              <LayoutDashboardIcon className="size-4" /> Admin dashboard
            </Link>
          )}
          <Link
            href="/shares"
            onClick={() => setOpen(false)}
            className="flex items-center gap-2 rounded-lg px-2 py-2 text-sm hover:bg-muted"
          >
            <HandCoinsIcon className="size-4" /> Split payments
          </Link>
          <Link
            href="/planner"
            onClick={() => setOpen(false)}
            className="flex items-center gap-2 rounded-lg px-2 py-2 text-sm hover:bg-muted"
          >
            <CalendarRangeIcon className="size-4" /> Weekend planner
          </Link>
          <Link
            href="/polls"
            onClick={() => setOpen(false)}
            className="flex items-center gap-2 rounded-lg px-2 py-2 text-sm hover:bg-muted"
          >
            <VoteIcon className="size-4" /> Group polls
          </Link>
          <Link
            href="/friends"
            onClick={() => setOpen(false)}
            className="flex items-center gap-2 rounded-lg px-2 py-2 text-sm hover:bg-muted"
          >
            <UsersIcon className="size-4" /> Friends
          </Link>
          <Link
            href="/alerts"
            onClick={() => setOpen(false)}
            className="flex items-center gap-2 rounded-lg px-2 py-2 text-sm hover:bg-muted"
          >
            <BellIcon className="size-4" /> Price alerts
          </Link>
          <button
            type="button"
            className="flex w-full items-center gap-2 rounded-lg px-2 py-2 text-sm hover:bg-muted"
            onClick={() =>
              logout.mutate(undefined, {
                onSuccess: () => {
                  setOpen(false);
                  router.push("/");
                },
              })
            }
          >
            <LogOutIcon className="size-4" /> Sign out
          </button>
        </div>
      )}
    </div>
  );
}
