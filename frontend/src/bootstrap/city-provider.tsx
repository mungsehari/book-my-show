"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

const STORAGE_KEY = "bms.cityId";

type CityContextValue = {
  cityId: number | null;
  setCityId: (id: number) => void;
};

const CityContext = createContext<CityContextValue | null>(null);

export function CityProvider({ children }: { children: ReactNode }) {
  const [cityId, setCityState] = useState<number | null>(null);

  useEffect(() => {
    try {
      const saved = window.localStorage.getItem(STORAGE_KEY);
      // eslint-disable-next-line react-hooks/set-state-in-effect
      if (saved) setCityState(Number(saved));
    } catch {
      // Storage can be blocked. The app works without it.
    }
  }, []);

  const setCityId = useCallback((id: number) => {
    setCityState(id);
    try {
      window.localStorage.setItem(STORAGE_KEY, String(id));
    } catch {
      // Ignore storage errors.
    }
  }, []);

  const value = useMemo(() => ({ cityId, setCityId }), [cityId, setCityId]);
  return <CityContext.Provider value={value}>{children}</CityContext.Provider>;
}

export function useSelectedCity(): CityContextValue {
  const context = useContext(CityContext);
  if (!context) throw new Error("useSelectedCity needs CityProvider");
  return context;
}
