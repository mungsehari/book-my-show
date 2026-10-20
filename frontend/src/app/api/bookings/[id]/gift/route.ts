import { forwardAuthed } from "@/lib/server/backend";

export async function POST(request: Request, ctx: RouteContext<"/api/bookings/[id]/gift">) {
  const { id } = await ctx.params;
  const body = await request.json().catch(() => ({}));
  return forwardAuthed(`/bookings/${encodeURIComponent(id)}/gift`, { method: "POST", body });
}
