import { forwardAuthed } from "@/lib/server/backend";

export async function POST(request: Request, ctx: RouteContext<"/api/movies/[id]/reviews">) {
  const { id } = await ctx.params;
  const body = await request.json().catch(() => ({}));
  return forwardAuthed(`/movies/${encodeURIComponent(id)}/reviews`, {
    method: "POST",
    body,
  });
}
