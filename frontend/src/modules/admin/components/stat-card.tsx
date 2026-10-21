import type { LucideIcon } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { CountUp } from "@/modules/admin/components/count-up";

type Props = {
  label: string;
  icon: LucideIcon;
  tint: string;
  value?: number;
  format?: (value: number) => string;
  note?: string;
  delay: number;
};

export function StatCard({ label, icon: Icon, tint, value, format, note, delay }: Props) {
  return (
    <div
      className="animate-fade-up rounded-2xl border bg-card p-4 shadow-sm transition duration-300 hover:-translate-y-1 hover:shadow-lg"
      style={{ ["--delay" as string]: `${delay}ms` }}
    >
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">{label}</p>
        <span className={`flex size-9 items-center justify-center rounded-xl bg-gradient-to-br text-white ${tint}`}>
          <Icon className="size-4" />
        </span>
      </div>
      <div className="mt-2 text-2xl font-extrabold sm:text-3xl">
        {value === undefined ? <Skeleton className="h-8 w-28" /> : <CountUp value={value} format={format} />}
      </div>
      {note && <p className="mt-1 text-xs text-muted-foreground">{note}</p>}
    </div>
  );
}
