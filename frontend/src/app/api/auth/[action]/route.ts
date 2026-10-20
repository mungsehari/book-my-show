import { backendFetch, relay } from "@/lib/server/backend";
import { clearSession, setSession } from "@/lib/server/session";

/** Browser action name to backend path. These calls give back a token. */
const SIGN_IN_PATHS: Record<string, string> = {
  login: "/auth/login",
  register: "/auth/register",
  "otp-verify": "/auth/otp/verify",
  google: "/auth/google",
};

const PLAIN_PATHS: Record<string, string> = {
  "otp-request": "/auth/otp/request",
};

type AuthBody = {
  accessToken: string;
  expiresAt: string;
  user: unknown;
};

export async function POST(request: Request, ctx: RouteContext<"/api/auth/[action]">) {
  const { action } = await ctx.params;

  if (action === "logout") {
    await clearSession();
    return new Response(null, { status: 204 });
  }

  const body = await request.json().catch(() => ({}));

  const plainPath = PLAIN_PATHS[action];
  if (plainPath) {
    return relay(await backendFetch(plainPath, { method: "POST", body }));
  }

  const signInPath = SIGN_IN_PATHS[action];
  if (!signInPath) return Response.json({ message: "Unknown action." }, { status: 404 });

  const response = await backendFetch(signInPath, { method: "POST", body });
  if (!response.ok) return relay(response);

  const auth = (await response.json()) as AuthBody;
  await setSession(auth.accessToken, auth.expiresAt);
  return Response.json(auth.user);
}
