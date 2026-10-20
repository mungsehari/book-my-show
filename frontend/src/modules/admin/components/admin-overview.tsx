"use client";

import { ArrowRightIcon } from "lucide-react";
import Link from "next/link";
import { ADMIN_LINKS } from "@/modules/admin/components/admin-shell";
import { useAdminRole } from "@/modules/admin/hooks";
import { useMe } from "@/modules/auth/hooks";

const TINTS = [
  "from-rose-500 to-orange-400",
  "from-violet-500 to-fuchsia-400",
  "from-sky-500 to-cyan-400",
  "from-emerald-500 to-lime-400",
  "from-amber-500 to-red-400",
  "from-indigo-500 to-blue-400",
];

const HINTS: Record<string, string> = {
  "/admin/movies/new": "Put a new movie in the catalog",
  "/admin/screens/new": "Design a screen and its seats",
  "/admin/shows/new": "Schedule a show with prices",
  "/admin/scan": "Check in a ticket at the entry",
  "/admin/extras": "Food, parking and seat view photos",
  "/admin/manage": "Make theatre admins and refund bookings",
};

export function AdminOverview() {
  const { data: user } = useMe();
  const { isAdmin } = useAdminRole();
  const cards = ADMIN_LINKS.filter((link) => link.href !== "/admin" && (isAdmin || !link.adminOnly));

  return (
    <div className="space-y-8">
      <section className="animate-fade-up relative overflow-hidden rounded-3xl bg-gradient-to-br from-zinc-900 via-zinc-800 to-rose-950 p-6 text-white shadow-xl sm:p-8">
        <p className="text-sm text-white/70">{isAdmin ? "Admin" : "Theatre admin"}</p>
        <h1 className="mt-1 text-2xl font-extrabold sm:text-3xl">
          Hello{user ? `, ${user.name}` : ""}
        </h1>
        <p className="mt-2 max-w-lg text-sm text-white/75">
          Add movies, build screens, schedule shows and check in tickets. Pick a task below.
        </p>
      </section>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {cards.map((card, index) => (
          <Link
            key={card.href}
            href={card.href}
            className="animate-fade-up group rounded-2xl border bg-card p-5 shadow-sm transition duration-300 hover:-translate-y-1.5 hover:shadow-xl"
            style={{ ["--delay" as string]: `${index * 70}ms` }}
          >
            <span
              className={`flex size-11 items-center justify-center rounded-xl bg-gradient-to-br text-white shadow-md transition group-hover:scale-110 group-hover:-rotate-6 ${TINTS[index % TINTS.length]}`}
            >
              <card.icon className="size-5" />
            </span>
            <h2 className="mt-4 font-semibold">{card.label}</h2>
            <p className="text-sm text-muted-foreground">{HINTS[card.href]}</p>
            <span className="mt-3 flex items-center gap-1 text-sm font-medium text-primary opacity-0 transition group-hover:opacity-100">
              Open <ArrowRightIcon className="size-4" />
            </span>
          </Link>
        ))}
      </div>
    </div>
  );
}
