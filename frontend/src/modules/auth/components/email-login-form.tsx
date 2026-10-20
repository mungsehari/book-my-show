"use client";

import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { FormField } from "@/modules/auth/components/form-field";
import { useLogin } from "@/modules/auth/hooks";
import type { LoginInput } from "@/modules/auth/types";

export function EmailLoginForm({ onDone }: { onDone: () => void }) {
  const { register, handleSubmit, formState } = useForm<LoginInput>();
  const mutation = useLogin();
  const { errors } = formState;

  return (
    <form
      className="space-y-4"
      onSubmit={handleSubmit((values) => mutation.mutate(values, { onSuccess: onDone }))}
    >
      <FormField
        id="login-email"
        label="Email"
        type="email"
        autoComplete="email"
        error={errors.email?.message}
        {...register("email", { required: "Enter your email" })}
      />
      <FormField
        id="login-password"
        label="Password"
        type="password"
        autoComplete="current-password"
        error={errors.password?.message}
        {...register("password", { required: "Enter your password" })}
      />
      {mutation.error && (
        <p className="text-sm text-destructive">{mutation.error.message}</p>
      )}
      <Button type="submit" className="w-full" disabled={mutation.isPending}>
        {mutation.isPending ? "Signing in..." : "Sign in"}
      </Button>
    </form>
  );
}
