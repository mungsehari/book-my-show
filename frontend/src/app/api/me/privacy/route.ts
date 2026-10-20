import { forwardAuthed } from "@/lib/server/backend";

export async function GET() {
  return forwardAuthed("/me/privacy");
}

export async function POST(request: Request) {
  const body = await request.json().catch(() => ({}));
  return forwardAuthed("/me/privacy", { method: "POST", body });
}
