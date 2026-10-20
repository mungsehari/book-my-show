import "server-only";
import { getToken } from "@/lib/server/session";

const API_URL = process.env.API_URL ?? "http://localhost:8081";

type Options = {
  method?: string;
  body?: unknown;
  query?: string;
  withToken?: boolean;
};

/** Calls the Spring backend. Adds the session token when asked. */
export async function backendFetch(path: string, options: Options = {}): Promise<Response> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (options.body !== undefined) headers["Content-Type"] = "application/json";
  if (options.withToken) {
    const token = await getToken();
    if (token) headers.Authorization = `Bearer ${token}`;
  }
  const url = `${API_URL}/api/v1${path}${options.query ? `?${options.query}` : ""}`;
  try {
    return await fetch(url, {
      method: options.method ?? "GET",
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      cache: "no-store",
    });
  } catch {
    return Response.json(
      { status: 502, message: "Cannot reach the server. Please try again." },
      { status: 502 },
    );
  }
}

/** Same as backendFetch with the token, but answers 401 when nobody is signed in. */
export async function forwardAuthed(path: string, options: Options = {}): Promise<Response> {
  if (!(await getToken())) return new Response(null, { status: 401 });
  return relay(await backendFetch(path, { ...options, withToken: true }));
}

/** Passes the backend answer to the browser as it is. */
export async function relay(response: Response): Promise<Response> {
  const text = await response.text();
  return new Response(text || null, {
    status: response.status,
    headers: { "Content-Type": "application/json" },
  });
}
