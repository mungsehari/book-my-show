import { backendFetch, relay } from "@/lib/server/backend";
import { getToken } from "@/lib/server/session";

export async function POST(request: Request, ctx: RouteContext<"/api/shows/[id]/locks">) {
  if (!(await getToken())) return new Response(null, { status: 401 });
  const { id } = await ctx.params;
  const body = await request.json().catch(() => ({}));
  return relay(
    await backendFetch(`/shows/${encodeURIComponent(id)}/locks`, {
      method: "POST",
      body,
      withToken: true,
    }),
  );
}
