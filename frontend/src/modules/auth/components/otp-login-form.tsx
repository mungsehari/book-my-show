"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { FormField } from "@/modules/auth/components/form-field";
import { useRequestOtp, useVerifyOtp } from "@/modules/auth/hooks";

const PHONE_RULE = {
  required: "Enter your phone number",
  pattern: {
    value: /^\+[1-9][0-9]{7,14}$/,
    message: "Use international format, for example +919876543210",
  },
};

type PhoneValues = { phone: string };
type CodeValues = { code: string; name: string };

export function OtpLoginForm({ onDone }: { onDone: () => void }) {
  const [phone, setPhone] = useState<string | null>(null);

  if (phone === null) return <PhoneStep onSent={setPhone} />;
  return <CodeStep phone={phone} onBack={() => setPhone(null)} onDone={onDone} />;
}

function PhoneStep({ onSent }: { onSent: (phone: string) => void }) {
  const { register, handleSubmit, formState } = useForm<PhoneValues>();
  const mutation = useRequestOtp();

  return (
    <form
      className="space-y-4"
      onSubmit={handleSubmit(({ phone }) =>
        mutation.mutate(phone, { onSuccess: () => onSent(phone) }),
      )}
    >
      <FormField
        id="otp-phone"
        label="Phone number"
        type="tel"
        placeholder="+919876543210"
        autoComplete="tel"
        error={formState.errors.phone?.message}
        {...register("phone", PHONE_RULE)}
      />
      {mutation.error && (
        <p className="text-sm text-destructive">{mutation.error.message}</p>
      )}
      <Button type="submit" className="w-full" disabled={mutation.isPending}>
        {mutation.isPending ? "Sending code..." : "Send code"}
      </Button>
    </form>
  );
}

type CodeStepProps = { phone: string; onBack: () => void; onDone: () => void };

function CodeStep({ phone, onBack, onDone }: CodeStepProps) {
  const { register, handleSubmit, formState } = useForm<CodeValues>();
  const mutation = useVerifyOtp();
  const { errors } = formState;

  return (
    <form
      className="animate-fade-up space-y-4"
      onSubmit={handleSubmit(({ code, name }) =>
        mutation.mutate({ phone, code, name: name || undefined }, { onSuccess: onDone }),
      )}
    >
      <p className="text-sm text-muted-foreground">
        We sent a 6-digit code to <strong>{phone}</strong>.{" "}
        <button type="button" className="text-primary underline" onClick={onBack}>
          Change
        </button>
      </p>
      <FormField
        id="otp-code"
        label="6-digit code"
        inputMode="numeric"
        autoComplete="one-time-code"
        maxLength={6}
        error={errors.code?.message}
        {...register("code", {
          required: "Enter the code",
          pattern: { value: /^[0-9]{6}$/, message: "The code has 6 digits" },
        })}
      />
      <FormField
        id="otp-name"
        label="Your name (only if this is your first time)"
        autoComplete="name"
        error={errors.name?.message}
        {...register("name", { maxLength: 100 })}
      />
      {mutation.error && (
        <p className="text-sm text-destructive">{mutation.error.message}</p>
      )}
      <Button type="submit" className="w-full" disabled={mutation.isPending}>
        {mutation.isPending ? "Checking..." : "Verify and sign in"}
      </Button>
    </form>
  );
}
