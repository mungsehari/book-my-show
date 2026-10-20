import { backendFetch, relay } from "@/lib/server/backend";
import { clearSession, getToken } from "@/lib/server/session";

export async function GET() {
  if (!(await getToken())) return new Response(null, { status: 401 });
  const response = await backendFetch("/auth/me", { withToken: true });
  if (response.status === 401) await clearSession();
  return relay(response);
}
