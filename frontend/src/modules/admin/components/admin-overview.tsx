"use client";

import { ArrowRightIcon } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { ADMIN_LINKS } from "@/modules/admin/components/admin-shell";
import { DashboardPanel } from "@/modules/admin/components/dashboard-panel";
import { DashboardStats } from "@/modules/admin/components/dashboard-stats";
import { OccupancyList } from "@/modules/admin/components/occupancy-list";
import { RangeToggle } from "@/modules/admin/components/range-toggle";
import { RecentBookings } from "@/modules/admin/components/recent-bookings";
import { RefundsPending } from "@/modules/admin/components/refunds-pending";
import { RevenueChart } from "@/modules/admin/components/revenue-chart";
import { TopMovies } from "@/modules/admin/components/top-movies";
import { useAdminRole } from "@/modules/admin/hooks";
import { RANGES, type Range } from "@/modules/admin/types";
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
  const [days, setDays] = useState<Range>(RANGES[0]);
  const cards = ADMIN_LINKS.filter((link) => link.href !== "/admin" && (isAdmin || !link.adminOnly));

  return (
    <div className="space-y-8">
      <section className="animate-fade-up flex flex-wrap items-end justify-between gap-4 overflow-hidden rounded-3xl bg-gradient-to-br from-zinc-900 via-zinc-800 to-rose-950 p-6 text-white shadow-xl sm:p-8">
        <div>
          <p className="text-sm text-white/70">{isAdmin ? "Admin" : "Theatre admin"}</p>
          <h1 className="mt-1 text-2xl font-extrabold sm:text-3xl">Hello{user ? `, ${user.name}` : ""}</h1>
          <p className="mt-2 max-w-lg text-sm text-white/75">
            {isAdmin ? "Numbers for every cinema." : "Numbers for your cinemas."}
          </p>
        </div>
        <div className="rounded-xl bg-white/10 p-1 text-foreground backdrop-blur [&_button]:text-white [&_button[aria-selected=true]]:text-foreground">
          <RangeToggle value={days} onChange={setDays} />
        </div>
      </section>

      <DashboardStats days={days} />

      <div className="grid gap-6 xl:grid-cols-5">
        <DashboardPanel title="Revenue" hint="Sales per day" className="xl:col-span-3">
          <RevenueChart days={days} />
        </DashboardPanel>
        <DashboardPanel title="Top movies" hint="By revenue" className="xl:col-span-2">
          <TopMovies days={days} />
        </DashboardPanel>
      </div>

      <div className="grid gap-6 xl:grid-cols-2">
        <DashboardPanel title="How full are the shows" hint="Sold and held seats">
          <OccupancyList />
        </DashboardPanel>
        <div className="space-y-6">
          <DashboardPanel title="Refunds waiting" hint="Paid after the seats were sold">
            <RefundsPending />
          </DashboardPanel>
          <DashboardPanel title="Recent bookings">
            <RecentBookings />
          </DashboardPanel>
        </div>
      </div>

      <section className="space-y-3">
        <h2 className="text-lg font-semibold">Quick actions</h2>
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
              <h3 className="mt-4 font-semibold">{card.label}</h3>
              <p className="text-sm text-muted-foreground">{HINTS[card.href]}</p>
              <span className="mt-3 flex items-center gap-1 text-sm font-medium text-primary opacity-0 transition group-hover:opacity-100">
                Open <ArrowRightIcon className="size-4" />
              </span>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}
