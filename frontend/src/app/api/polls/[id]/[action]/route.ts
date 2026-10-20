import { forwardAuthed } from "@/lib/server/backend";

const ACTIONS = new Set(["vote", "close"]);

export async function POST(request: Request, ctx: RouteContext<"/api/polls/[id]/[action]">) {
  const { id, action } = await ctx.params;
  if (!ACTIONS.has(action)) return Response.json({ message: "Unknown action." }, { status: 404 });
  const body = action === "vote" ? await request.json().catch(() => ({})) : undefined;
  return forwardAuthed(`/polls/${encodeURIComponent(id)}/${action}`, {
    method: "POST",
    body,
  });
}
