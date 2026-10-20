const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8081";

export type ListResponse<T> = { items: T[]; count: number };

export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
};

type ApiErrorBody = {
  status?: number;
  error?: string;
  message?: string;
  requestId?: string;
};

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly requestId?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

type Query = Record<string, string | number | boolean | undefined | null>;

function withQuery(url: URL, query?: Query): string {
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== null && value !== "") {
      url.searchParams.set(key, String(value));
    }
  }
  return url.toString();
}

async function send<T>(url: string, init: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(url, init);
  } catch {
    throw new ApiError(0, "Cannot reach the server. Please try again.");
  }
  if (!response.ok) {
    const body = (await response.json().catch(() => ({}))) as ApiErrorBody;
    throw new ApiError(
      response.status,
      body.message ?? "Something went wrong.",
      body.requestId,
    );
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

/** Public backend call from the browser. */
export function apiGet<T>(path: string, query?: Query): Promise<T> {
  const url = withQuery(new URL(`/api/v1${path}`, BASE_URL), query);
  return send<T>(url, { headers: { Accept: "application/json" } });
}

/** Call to our own Next route handlers. They add the session token. */
export function appGet<T>(path: string, query?: Query): Promise<T> {
  const url = withQuery(new URL(path, window.location.origin), query);
  return send<T>(url, { headers: { Accept: "application/json" } });
}

export function appPost<T>(path: string, body?: unknown): Promise<T> {
  return send<T>(path, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}
