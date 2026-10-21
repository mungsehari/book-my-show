import type { ReactNode } from "react";

type Props = { title: string; hint?: string; children: ReactNode; className?: string };

/** A titled card used by every dashboard section. */
export function DashboardPanel({ title, hint, children, className }: Props) {
  return (
    <section className={`animate-fade-up space-y-4 rounded-3xl border bg-card p-5 shadow-sm ${className ?? ""}`}>
      <div>
        <h2 className="font-semibold">{title}</h2>
        {hint && <p className="text-sm text-muted-foreground">{hint}</p>}
      </div>
      {children}
    </section>
  );
}
