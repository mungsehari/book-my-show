import { backendFetch, relay } from "@/lib/server/backend";
import { getToken } from "@/lib/server/session";

export async function POST(_request: Request, ctx: RouteContext<"/api/locks/[id]/release">) {
  if (!(await getToken())) return new Response(null, { status: 401 });
  const { id } = await ctx.params;
  return relay(
    await backendFetch(`/locks/${encodeURIComponent(id)}/release`, {
      method: "POST",
      withToken: true,
    }),
  );
}
