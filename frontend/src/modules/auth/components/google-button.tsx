"use client";

import { useEffect, useRef, useState } from "react";
import { GOOGLE_CLIENT_ID, renderGoogleButton } from "@/lib/google";
import { useGoogleLogin } from "@/modules/auth/hooks";

/** Google button with an "or" line. Renders nothing when no client id is set. */
export function GoogleButton({ onDone }: { onDone: () => void }) {
  const holder = useRef<HTMLDivElement>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const mutation = useGoogleLogin();
  const { mutate } = mutation;

  useEffect(() => {
    const element = holder.current;
    if (!GOOGLE_CLIENT_ID || !element) return;
    renderGoogleButton(element, (idToken) => mutate(idToken, { onSuccess: onDone })).catch(
      (error: Error) => setLoadError(error.message),
    );
  }, [mutate, onDone]);

  if (!GOOGLE_CLIENT_ID) return null;

  const error = loadError ?? mutation.error?.message;
  return (
    <div className="space-y-3">
      <div ref={holder} className="flex min-h-11 justify-center" />
      {mutation.isPending && (
        <p className="text-center text-sm text-muted-foreground">Signing you in...</p>
      )}
      {error && <p className="text-center text-sm text-destructive">{error}</p>}
      <div className="flex items-center gap-3 text-xs text-muted-foreground">
        <span className="h-px flex-1 bg-border" />
        or use email or phone
        <span className="h-px flex-1 bg-border" />
      </div>
    </div>
  );
}
