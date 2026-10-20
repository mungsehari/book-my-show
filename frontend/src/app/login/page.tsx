import { Suspense } from "react";
import { AuthCard } from "@/modules/auth/components/auth-card";

async function LoginFromParams({ searchParams }: PageProps<"/login">) {
  const { next } = await searchParams;
  return <AuthCard next={typeof next === "string" ? next : null} initial="email" />;
}

export default function LoginPage(props: PageProps<"/login">) {
  return (
    <Suspense fallback={null}>
      <LoginFromParams {...props} />
    </Suspense>
  );
}
