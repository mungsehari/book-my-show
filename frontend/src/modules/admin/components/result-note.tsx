import { CheckCircle2Icon, XCircleIcon } from "lucide-react";

type Props = { success?: string | null; error?: string | null };

/** A small success or error banner that pops in. */
export function ResultNote({ success, error }: Props) {
  if (error) {
    return (
      <p className="animate-pop-in flex items-center gap-2 rounded-xl bg-destructive/10 p-3 text-sm text-destructive">
        <XCircleIcon className="size-4 shrink-0" /> {error}
      </p>
    );
  }
  if (success) {
    return (
      <p className="animate-pop-in flex items-center gap-2 rounded-xl bg-emerald-500/10 p-3 text-sm text-emerald-600">
        <CheckCircle2Icon className="size-4 shrink-0" /> {success}
      </p>
    );
  }
  return null;
}
