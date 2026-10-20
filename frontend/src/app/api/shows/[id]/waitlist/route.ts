import { forwardAuthed } from "@/lib/server/backend";

export async function GET(_request: Request, ctx: RouteContext<"/api/shows/[id]/waitlist">) {
  const { id } = await ctx.params;
  return forwardAuthed("/waitlist/me", { query: `showId=${encodeURIComponent(id)}` });
}

export async function POST(request: Request, ctx: RouteContext<"/api/shows/[id]/waitlist">) {
  const { id } = await ctx.params;
  const body = await request.json().catch(() => ({}));
  return forwardAuthed(`/shows/${encodeURIComponent(id)}/waitlist`, {
    method: "POST",
    body,
  });
}
