"use client";

import Link from "next/link";
import { useAuth } from "@/features/auth/auth-context";

export default function AuthNav() {
  const auth = useAuth();

  if (auth.status === "loading") return null;

  if (auth.status === "authenticated") {
    return (
      <div className="auth-nav">
        <span className="auth-nav-user">{auth.user?.nickname}</span>
        <Link href="/profile">프로필</Link>
        <button type="button" className="auth-nav-logout" onClick={() => { void auth.logout(); }}>로그아웃</button>
      </div>
    );
  }

  return (
    <div className="auth-nav">
      <Link href="/login">로그인</Link>
      <Link href="/signup">회원가입</Link>
    </div>
  );
}
