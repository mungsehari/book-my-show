import type { ComponentProps } from "react";
import { Input } from "@/components/ui/input";

type Props = ComponentProps<typeof Input> & {
  label: string;
  error?: string;
};

export function FormField({ label, error, id, ...inputProps }: Props) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      <Input id={id} aria-invalid={!!error} {...inputProps} />
      {error && <p className="animate-pop-in text-xs text-destructive">{error}</p>}
    </div>
  );
}
