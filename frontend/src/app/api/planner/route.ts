import { forwardAuthed } from "@/lib/server/backend";

export async function POST(request: Request) {
  const body = await request.json().catch(() => ({}));
  return forwardAuthed("/planner", { method: "POST", body });
}
