import { NextResponse, type NextRequest } from "next/server";

const SESSION_COOKIE = "bms_token";

/** Sends people without a session cookie to the login page. */
export function proxy(request: NextRequest) {
  if (request.cookies.has(SESSION_COOKIE)) return NextResponse.next();
  const login = new URL("/login", request.url);
  login.searchParams.set("next", request.nextUrl.pathname);
  return NextResponse.redirect(login);
}

export const config = {
  matcher: ["/bookings/:path*", "/shows/:path*", "/alerts", "/friends", "/polls/:path*", "/planner", "/shares"],
};
