import { forwardAuthed } from "@/lib/server/backend";

export async function GET(request: Request) {
  const query = new URL(request.url).searchParams.toString();
  return forwardAuthed("/bookings/me", { query });
}

export async function POST(request: Request) {
  const body = await request.json().catch(() => ({}));
  return forwardAuthed("/bookings", { method: "POST", body });
}
