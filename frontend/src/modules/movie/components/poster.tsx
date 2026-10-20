import { ClapperboardIcon } from "lucide-react";
import { cn } from "@/lib/utils";

const GRADIENTS = [
  "from-rose-500 to-orange-400",
  "from-violet-600 to-fuchsia-500",
  "from-sky-600 to-cyan-400",
  "from-emerald-600 to-lime-400",
  "from-amber-500 to-red-500",
  "from-indigo-600 to-blue-400",
];

function gradientFor(title: string): string {
  let hash = 0;
  for (const char of title) hash = (hash * 31 + char.charCodeAt(0)) % 997;
  return GRADIENTS[hash % GRADIENTS.length];
}

type Props = {
  title: string;
  posterUrl: string | null;
  className?: string;
};

/** Shows the poster, or a gradient with the title when there is no image. */
export function Poster({ title, posterUrl, className }: Props) {
  if (posterUrl) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={posterUrl}
        alt={`${title} poster`}
        className={cn("h-full w-full object-cover", className)}
      />
    );
  }
  return (
    <div
      className={cn(
        "flex h-full w-full flex-col items-center justify-center gap-3 bg-gradient-to-br p-4 text-center text-white",
        gradientFor(title),
        className,
      )}
    >
      <ClapperboardIcon className="size-10 opacity-80" />
      <span className="text-lg font-bold leading-tight drop-shadow">{title}</span>
    </div>
  );
}
