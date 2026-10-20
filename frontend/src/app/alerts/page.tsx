import { AlertsList } from "@/modules/alert/components/alerts-list";

export default function AlertsPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">My price alerts</h1>
      <AlertsList />
    </div>
  );
}
