"use client";

import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { FormField } from "@/modules/auth/components/form-field";
import { useRegister } from "@/modules/auth/hooks";
import type { RegisterInput } from "@/modules/auth/types";

export function RegisterForm({ onDone }: { onDone: () => void }) {
  const { register, handleSubmit, formState } = useForm<RegisterInput>();
  const mutation = useRegister();
  const { errors } = formState;

  return (
    <form
      className="space-y-4"
      onSubmit={handleSubmit((values) => mutation.mutate(values, { onSuccess: onDone }))}
    >
      <FormField
        id="register-name"
        label="Name"
        autoComplete="name"
        error={errors.name?.message}
        {...register("name", { required: "Enter your name", maxLength: 100 })}
      />
      <FormField
        id="register-email"
        label="Email"
        type="email"
        autoComplete="email"
        error={errors.email?.message}
        {...register("email", { required: "Enter your email" })}
      />
      <FormField
        id="register-password"
        label="Password (8 characters or more)"
        type="password"
        autoComplete="new-password"
        error={errors.password?.message}
        {...register("password", {
          required: "Choose a password",
          minLength: { value: 8, message: "Use at least 8 characters" },
          maxLength: { value: 72, message: "Use at most 72 characters" },
        })}
      />
      {mutation.error && (
        <p className="text-sm text-destructive">{mutation.error.message}</p>
      )}
      <Button type="submit" className="w-full" disabled={mutation.isPending}>
        {mutation.isPending ? "Creating account..." : "Create account"}
      </Button>
    </form>
  );
}
