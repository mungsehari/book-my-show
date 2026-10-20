import { SharesList } from "@/modules/booking/components/shares-list";

export default function SharesPage() {
  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Split payments</h1>
        <p className="text-sm text-muted-foreground">Shares your friends are waiting for you to pay.</p>
      </div>
      <SharesList />
    </div>
  );
}
