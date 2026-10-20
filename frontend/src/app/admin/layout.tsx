import type { ReactNode } from "react";
import { AdminShell } from "@/modules/admin/components/admin-shell";

export default function AdminLayout({ children }: { children: ReactNode }) {
  return <AdminShell>{children}</AdminShell>;
}
