import { forwardAuthed } from "@/lib/server/backend";

export async function GET(_request: Request, ctx: RouteContext<"/api/polls/[id]">) {
  const { id } = await ctx.params;
  return forwardAuthed(`/polls/${encodeURIComponent(id)}`);
}
