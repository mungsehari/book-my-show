import { AssignAdminForm, RefundForm } from "@/modules/admin/components/manage-forms";

export default function ManagePage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">Access and refunds</h1>
      <div className="grid gap-6 xl:grid-cols-2">
        <AssignAdminForm />
        <RefundForm />
      </div>
    </div>
  );
}
