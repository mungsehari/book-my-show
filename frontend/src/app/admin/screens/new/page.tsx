import { ScreenBuilder } from "@/modules/admin/components/screen-builder";

export default function NewScreenPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Build a screen</h1>
        <p className="text-sm text-muted-foreground">
          Add rows, set seats and seat type. The preview updates as you go.
        </p>
      </div>
      <ScreenBuilder />
    </div>
  );
}
