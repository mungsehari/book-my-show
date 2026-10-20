import { ScanTicket } from "@/modules/admin/components/scan-ticket";

export default function ScanPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Scan a ticket</h1>
        <p className="text-sm text-muted-foreground">Each ticket works once, on the show day.</p>
      </div>
      <ScanTicket />
    </div>
  );
}
