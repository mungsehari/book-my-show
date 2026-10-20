const COLORS = [
  "bg-rose-500",
  "bg-violet-500",
  "bg-sky-500",
  "bg-emerald-500",
  "bg-amber-500",
  "bg-indigo-500",
];

/** Round letter badge with a color picked from the user id. */
export function Avatar({ name, userId }: { name: string; userId: number }) {
  return (
    <span
      className={`flex size-9 shrink-0 items-center justify-center rounded-full font-semibold text-white ${COLORS[userId % COLORS.length]}`}
      aria-hidden
    >
      {name.charAt(0).toUpperCase()}
    </span>
  );
}
