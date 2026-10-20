import { forwardAuthed } from "@/lib/server/backend";

export async function GET(_request: Request, ctx: RouteContext<"/api/bookings/[id]">) {
  const { id } = await ctx.params;
  return forwardAuthed(`/bookings/${encodeURIComponent(id)}`);
}
