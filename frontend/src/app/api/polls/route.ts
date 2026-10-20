import { forwardAuthed } from "@/lib/server/backend";

export async function GET() {
  return forwardAuthed("/polls/mine");
}

export async function POST(request: Request) {
  const body = await request.json().catch(() => ({}));
  return forwardAuthed("/polls", { method: "POST", body });
}
