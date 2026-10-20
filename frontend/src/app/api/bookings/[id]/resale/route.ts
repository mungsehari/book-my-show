import { forwardAuthed } from "@/lib/server/backend";

export async function POST(_request: Request, ctx: RouteContext<"/api/bookings/[id]/resale">) {
  const { id } = await ctx.params;
  return forwardAuthed(`/bookings/${encodeURIComponent(id)}/resale`, { method: "POST" });
}
