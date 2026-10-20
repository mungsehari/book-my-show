import { forwardAuthed } from "@/lib/server/backend";

export async function POST(request: Request, ctx: RouteContext<"/api/payment-shares/[id]/confirm">) {
  const { id } = await ctx.params;
  const body = await request.json().catch(() => ({}));
  return forwardAuthed(`/payment-shares/${encodeURIComponent(id)}/confirm`, {
    method: "POST",
    body,
  });
}
