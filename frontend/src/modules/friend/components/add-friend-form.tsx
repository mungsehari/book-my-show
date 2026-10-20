"use client";

import { UserPlusIcon } from "lucide-react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { FormField } from "@/modules/auth/components/form-field";
import { useSendRequest } from "@/modules/friend/hooks";

export function AddFriendForm() {
  const { register, handleSubmit, reset, formState } = useForm<{ email: string }>();
  const mutation = useSendRequest();

  return (
    <form
      className="space-y-3 rounded-2xl border bg-card p-4"
      onSubmit={handleSubmit(({ email }) =>
        mutation.mutate(email, { onSuccess: () => reset() }),
      )}
    >
      <h2 className="flex items-center gap-2 font-semibold">
        <UserPlusIcon className="size-5 text-primary" /> Add a friend
      </h2>
      <FormField
        id="friend-email"
        label="Their email"
        type="email"
        placeholder="friend@example.com"
        error={formState.errors.email?.message}
        {...register("email", { required: "Enter an email" })}
      />
      {mutation.error && <p className="text-sm text-destructive">{mutation.error.message}</p>}
      {mutation.isSuccess && (
        <p className="animate-pop-in text-sm text-emerald-600">
          {mutation.data.status === "ACCEPTED" ? "You are now friends!" : "Request sent."}
        </p>
      )}
      <Button type="submit" disabled={mutation.isPending}>
        {mutation.isPending ? "Sending..." : "Send request"}
      </Button>
    </form>
  );
}
