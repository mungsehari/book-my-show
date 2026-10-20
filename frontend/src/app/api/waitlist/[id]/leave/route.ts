import { forwardAuthed } from "@/lib/server/backend";

export async function POST(_request: Request, ctx: RouteContext<"/api/waitlist/[id]/leave">) {
  const { id } = await ctx.params;
  return forwardAuthed(`/waitlist/${encodeURIComponent(id)}/leave`, { method: "POST" });
}
