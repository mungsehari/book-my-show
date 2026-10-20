import { forwardAuthed } from "@/lib/server/backend";

export async function POST(_request: Request, ctx: RouteContext<"/api/resale/[id]/checkout">) {
  const { id } = await ctx.params;
  return forwardAuthed(`/resale/${encodeURIComponent(id)}/checkout`, { method: "POST" });
}
