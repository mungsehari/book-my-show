import { PlaceForm, ViewForm } from "@/modules/admin/components/extras-forms";

export default function ExtrasPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">Cinema extras</h1>
      <div className="grid gap-6 xl:grid-cols-2">
        <PlaceForm />
        <ViewForm />
      </div>
    </div>
  );
}
