"use client";

import {
  FilmIcon,
  LayoutDashboardIcon,
  LockIcon,
  PlusSquareIcon,
  ScanLineIcon,
  ShieldCheckIcon,
  SofaIcon,
  SparklesIcon,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { useAdminRole } from "@/modules/admin/hooks";

export const ADMIN_LINKS = [
  { href: "/admin", label: "Overview", icon: LayoutDashboardIcon, adminOnly: false },
  { href: "/admin/movies/new", label: "Add movie", icon: FilmIcon, adminOnly: true },
  { href: "/admin/screens/new", label: "Add screen", icon: SofaIcon, adminOnly: false },
  { href: "/admin/shows/new", label: "Add show", icon: PlusSquareIcon, adminOnly: false },
  { href: "/admin/scan", label: "Scan ticket", icon: ScanLineIcon, adminOnly: false },
  { href: "/admin/extras", label: "Cinema extras", icon: SparklesIcon, adminOnly: false },
  { href: "/admin/manage", label: "Access and refunds", icon: ShieldCheckIcon, adminOnly: true },
];

export function AdminShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const { isLoading, isAdmin, canManage } = useAdminRole();

  // The page is always rendered, only hidden until the role is known. Next needs the
  // segment to render, and the real check is on the backend.
  if (isLoading) {
    return (
      <>
        <Skeleton className="h-96 w-full rounded-3xl" />
        <div hidden>{children}</div>
      </>
    );
  }
  if (!canManage) {
    return (
      <>
        <div className="flex flex-col items-center gap-2 rounded-3xl border border-dashed py-20 text-center text-muted-foreground">
          <LockIcon className="size-10" />
          <p className="font-medium text-foreground">Admins only</p>
          <p className="text-sm">Your account does not have access to this area.</p>
        </div>
        <div hidden>{children}</div>
      </>
    );
  }

  const links = ADMIN_LINKS.filter((link) => isAdmin || !link.adminOnly);

  return (
    <div className="grid gap-6 lg:grid-cols-[14rem_1fr]">
      <nav
        aria-label="Admin"
        className="-mx-4 flex gap-1 overflow-x-auto px-4 lg:mx-0 lg:flex-col lg:px-0"
      >
        {links.map((link) => {
          const active = pathname === link.href;
          return (
            <Link
              key={link.href}
              href={link.href}
              className={cn(
                "flex shrink-0 items-center gap-2 rounded-xl px-3 py-2 text-sm font-medium transition hover:bg-muted",
                active && "bg-primary text-primary-foreground shadow hover:bg-primary",
              )}
            >
              <link.icon className="size-4" /> {link.label}
            </Link>
          );
        })}
      </nav>
      <div className="min-w-0">{children}</div>
    </div>
  );
}
