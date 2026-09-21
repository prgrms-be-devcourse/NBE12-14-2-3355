import { Suspense } from "react";
import type { Metadata } from "next";
import LoginClient from "./login-client";

export const metadata: Metadata = { title: "로그인 | GameLog" };

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginClient />
    </Suspense>
  );
}
