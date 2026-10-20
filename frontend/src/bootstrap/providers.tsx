"use client";

import type { ReactNode } from "react";
import { CityProvider } from "@/bootstrap/city-provider";
import { QueryProvider } from "@/bootstrap/query-provider";

export function Providers({ children }: { children: ReactNode }) {
  return (
    <QueryProvider>
      <CityProvider>{children}</CityProvider>
    </QueryProvider>
  );
}
