import { forwardAuthed } from "@/lib/server/backend";

export async function POST(_request: Request, ctx: RouteContext<"/api/friends/[id]/remove">) {
  const { id } = await ctx.params;
  return forwardAuthed(`/friends/${encodeURIComponent(id)}/remove`, { method: "POST" });
}
