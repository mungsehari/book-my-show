"use client";

import { StarIcon } from "lucide-react";
import { useState } from "react";
import { cn } from "@/lib/utils";

type DisplayProps = { value: number; size?: string };

/** Read-only stars. */
export function Stars({ value, size = "size-4" }: DisplayProps) {
  return (
    <span className="inline-flex" role="img" aria-label={`${value} out of 5`}>
      {[1, 2, 3, 4, 5].map((n) => (
        <StarIcon
          key={n}
          className={cn(size, n <= Math.round(value) ? "fill-amber-400 text-amber-400" : "text-muted-foreground/40")}
        />
      ))}
    </span>
  );
}

type InputProps = { value: number; onChange: (value: number) => void };

/** Stars the user can click. */
export function StarInput({ value, onChange }: InputProps) {
  const [hover, setHover] = useState(0);
  const shown = hover || value;
  return (
    <div className="inline-flex gap-1" role="radiogroup" aria-label="Your rating">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          role="radio"
          aria-checked={value === n}
          aria-label={`${n} star${n > 1 ? "s" : ""}`}
          onMouseEnter={() => setHover(n)}
          onMouseLeave={() => setHover(0)}
          onClick={() => onChange(n)}
          className="transition hover:scale-125"
        >
          <StarIcon
            className={cn(
              "size-7",
              n <= shown ? "fill-amber-400 text-amber-400" : "text-muted-foreground/40",
            )}
          />
        </button>
      ))}
    </div>
  );
}
