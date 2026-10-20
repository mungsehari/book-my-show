import { forwardAuthed } from "@/lib/server/backend";

export async function GET() {
  return forwardAuthed("/price-alerts/me");
}

export async function POST(request: Request) {
  const body = await request.json().catch(() => ({}));
  return forwardAuthed("/price-alerts", { method: "POST", body });
}
