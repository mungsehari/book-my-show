import { Button } from "@/components/ui/button";
import { formatPrice } from "@/lib/format";

type Props = {
  labels: string[];
  total: number;
  percentOff?: number;
  notice: string | null;
  error: string | null;
  locking: boolean;
  onProceed: () => void;
  onClear: () => void;
};

export function SummaryBar({
  labels,
  total,
  percentOff,
  notice,
  error,
  locking,
  onProceed,
  onClear,
}: Props) {
  const empty = labels.length === 0;
  return (
    <div className="sticky bottom-0 z-30 -mx-4 border-t bg-background/90 px-4 py-3 backdrop-blur-md">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3">
        <div className="min-w-0">
          {empty ? (
            <p className="text-sm text-muted-foreground">Tap seats to select them.</p>
          ) : (
            <>
              <p className="truncate text-sm font-medium">
                {labels.length} {labels.length === 1 ? "seat" : "seats"}: {labels.join(", ")}
              </p>
              <p className="text-lg font-extrabold">
                {formatPrice(total)}
                {percentOff ? (
                  <span className="ml-2 rounded-full bg-emerald-500/15 px-2 py-0.5 text-xs font-medium text-emerald-600 dark:text-emerald-400">
                    {percentOff}% off applied
                  </span>
                ) : null}
              </p>
            </>
          )}
          {(notice || error) && (
            <p className="animate-pop-in text-xs text-destructive">{error ?? notice}</p>
          )}
        </div>
        <div className="flex gap-2">
          {!empty && (
            <Button variant="ghost" onClick={onClear}>
              Clear
            </Button>
          )}
          <Button disabled={empty || locking} onClick={onProceed}>
            {locking ? "Holding seats..." : "Proceed"}
          </Button>
        </div>
      </div>
    </div>
  );
}
