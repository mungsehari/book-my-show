"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useState } from "react";
import { cn } from "@/lib/utils";
import { GoogleButton } from "@/modules/auth/components/google-button";
import { EmailLoginForm } from "@/modules/auth/components/email-login-form";
import { OtpLoginForm } from "@/modules/auth/components/otp-login-form";
import { RegisterForm } from "@/modules/auth/components/register-form";

type Mode = "email" | "phone" | "register";

const TABS: { mode: Mode; label: string }[] = [
  { mode: "email", label: "Email" },
  { mode: "phone", label: "Phone OTP" },
  { mode: "register", label: "New account" },
];

/** Only same-site paths are allowed, so ?next= cannot send people elsewhere. */
function safeNext(next: string | null): string {
  return next && next.startsWith("/") && !next.startsWith("//") ? next : "/";
}

export function AuthCard({ next, initial }: { next: string | null; initial: Mode }) {
  const [mode, setMode] = useState<Mode>(initial);
  const router = useRouter();
  const done = useCallback(() => router.replace(safeNext(next)), [router, next]);

  return (
    <div className="animate-fade-up mx-auto w-full max-w-md space-y-6 rounded-3xl border bg-card p-6 shadow-xl sm:p-8">
      <div className="space-y-1 text-center">
        <h1 className="text-2xl font-extrabold">
          {mode === "register" ? "Create your account" : "Welcome back"}
        </h1>
        <p className="text-sm text-muted-foreground">
          Sign in to book tickets and see your bookings.
        </p>
      </div>
      <GoogleButton onDone={done} />
      <div className="grid grid-cols-3 gap-1 rounded-xl bg-muted p-1" role="tablist">
        {TABS.map((tab) => (
          <button
            key={tab.mode}
            type="button"
            role="tab"
            aria-selected={mode === tab.mode}
            onClick={() => setMode(tab.mode)}
            className={cn(
              "rounded-lg px-2 py-2 text-sm font-medium transition",
              mode === tab.mode ? "bg-background shadow" : "text-muted-foreground",
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>
      {mode === "email" && <EmailLoginForm onDone={done} />}
      {mode === "phone" && <OtpLoginForm onDone={done} />}
      {mode === "register" && <RegisterForm onDone={done} />}
      <p className="text-center text-xs text-muted-foreground">
        <Link href="/" className="underline">
          Back to home
        </Link>
      </p>
    </div>
  );
}
