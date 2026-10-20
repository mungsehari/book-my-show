import { forwardAuthed } from "@/lib/server/backend";

export async function GET() {
  return forwardAuthed("/friends/requests");
}

export async function POST(request: Request) {
  const body = await request.json().catch(() => ({}));
  return forwardAuthed("/friends/requests", { method: "POST", body });
}
