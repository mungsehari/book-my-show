import { forwardAuthed } from "@/lib/server/backend";

export async function GET() {
  return forwardAuthed("/friends");
}
