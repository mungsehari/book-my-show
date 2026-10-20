import { forwardAuthed } from "@/lib/server/backend";

export async function GET(request: Request) {
  const query = new URL(request.url).searchParams.toString();
  return forwardAuthed("/friends/activity", { query });
}
