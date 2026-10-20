import { forwardAuthed } from "@/lib/server/backend";

export async function POST(_request: Request, ctx: RouteContext<"/api/price-alerts/[id]/cancel">) {
  const { id } = await ctx.params;
  return forwardAuthed(`/price-alerts/${encodeURIComponent(id)}/cancel`, { method: "POST" });
}
