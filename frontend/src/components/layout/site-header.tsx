import { TicketIcon } from "lucide-react";
import Link from "next/link";
import { ThemeToggle } from "@/components/layout/theme-toggle";
import { UserMenu } from "@/modules/auth/components/user-menu";
import { CityPicker } from "@/modules/city/components/city-picker";

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-40 border-b bg-background/80 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-3 px-4">
        <Link href="/" className="group flex items-center gap-2">
          <span className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-md transition group-hover:-rotate-12 group-hover:scale-110">
            <TicketIcon className="size-5" />
          </span>
          <span className="text-lg font-extrabold tracking-tight">
            book<span className="text-primary">my</span>show
          </span>
        </Link>
        <div className="flex items-center gap-2">
          <CityPicker />
          <ThemeToggle />
          <UserMenu />
        </div>
      </div>
    </header>
  );
}
