import { forwardAuthed } from "@/lib/server/backend";

/** Admin calls the browser may make. Anything else is refused here. */
const ALLOWED = [
  /^movies$/,
  /^cinemas\/\d+\/screens$/,
  /^cinemas\/\d+\/admins$/,
  /^cinemas\/\d+\/places$/,
  /^screens\/\d+\/shows$/,
  /^screens\/\d+\/views$/,
  /^tickets\/check-in$/,
  /^bookings\/\d+\/refund$/,
];

/** Admin reads the browser may make: the dashboard numbers and the bookings list. */
const ALLOWED_READS = [
  /^stats\/(summary|daily|top-movies|occupancy)$/,
  /^bookings$/,
];

export async function GET(request: Request, ctx: RouteContext<"/api/admin/[...path]">) {
  const { path } = await ctx.params;
  const joined = path.join("/");
  if (!ALLOWED_READS.some((pattern) => pattern.test(joined))) {
    return Response.json({ message: "Unknown admin action." }, { status: 404 });
  }
  const query = new URL(request.url).searchParams.toString();
  return forwardAuthed(`/admin/${joined}`, { query });
}

export async function POST(request: Request, ctx: RouteContext<"/api/admin/[...path]">) {
  const { path } = await ctx.params;
  const joined = path.join("/");
  if (!ALLOWED.some((pattern) => pattern.test(joined))) {
    return Response.json({ message: "Unknown admin action." }, { status: 404 });
  }
  const body = await request.json().catch(() => undefined);
  return forwardAuthed(`/admin/${joined}`, { method: "POST", body });
}
