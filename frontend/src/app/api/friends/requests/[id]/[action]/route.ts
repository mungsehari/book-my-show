import { forwardAuthed } from "@/lib/server/backend";

const ACTIONS = new Set(["accept", "decline"]);

export async function POST(
  _request: Request,
  ctx: RouteContext<"/api/friends/requests/[id]/[action]">,
) {
  const { id, action } = await ctx.params;
  if (!ACTIONS.has(action)) return Response.json({ message: "Unknown action." }, { status: 404 });
  return forwardAuthed(`/friends/requests/${encodeURIComponent(id)}/${action}`, {
    method: "POST",
  });
}
