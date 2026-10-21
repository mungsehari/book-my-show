"use client";

import { useEffect, useState } from "react";

type Props = {
  value: number;
  /** Turns the running number into text, for example as money. */
  format?: (value: number) => string;
  durationMs?: number;
};

function prefersReducedMotion(): boolean {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

/** A number that counts up from zero when it appears or changes. */
export function CountUp({ value, format = (n) => Math.round(n).toLocaleString("en-IN"), durationMs = 900 }: Props) {
  const [shown, setShown] = useState(0);

  useEffect(() => {
    const duration = prefersReducedMotion() ? 0 : durationMs;
    let start: number | null = null;
    let frame = 0;

    function step(now: number) {
      start ??= now;
      const progress = duration === 0 ? 1 : Math.min((now - start) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setShown(value * eased);
      if (progress < 1) frame = requestAnimationFrame(step);
    }

    frame = requestAnimationFrame(step);
    return () => cancelAnimationFrame(frame);
  }, [value, durationMs]);

  return <span className="tabular-nums">{format(shown)}</span>;
}
