const COLORS = ["bg-rose-500", "bg-amber-400", "bg-emerald-500", "bg-sky-500", "bg-violet-500"];

/** A short burst of falling pieces. Pure CSS, no library. */
export function Confetti() {
  return (
    <div className="pointer-events-none absolute inset-x-0 top-0 h-64 overflow-hidden" aria-hidden>
      {Array.from({ length: 24 }, (_, i) => (
        <span
          key={i}
          className={`animate-confetti absolute top-0 h-3 w-2 rounded-sm ${COLORS[i % COLORS.length]}`}
          style={{
            left: `${(i * 37) % 100}%`,
            ["--delay" as string]: `${(i * 90) % 700}ms`,
          }}
        />
      ))}
    </div>
  );
}
