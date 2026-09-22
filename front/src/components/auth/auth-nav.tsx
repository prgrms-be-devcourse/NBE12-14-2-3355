"use client";

import Link from "next/link";
import { useAuth } from "@/features/auth/auth-context";

export default function AuthNav() {
  const auth = useAuth();

  if (auth.status === "loading") return null;

  if (auth.status === "authenticated") {
    const isAdmin = auth.user?.role === "ADMIN";
    return (
      <div className="auth-nav">
{isAdmin && (
  <Link href="/admin/reports">신고 관리</Link>
)}
<Link href="/profile" className="auth-nav-avatar-link" aria-label="프로필">
  {auth.user?.profileImageUrl ? (
    // eslint-disable-next-line @next/next/no-img-element
    <img src={auth.user.profileImageUrl} alt="" className="auth-nav-avatar" />
  ) : (
    <span className="auth-nav-avatar-fallback">{auth.user?.nickname?.slice(0, 1).toUpperCase()}</span>
  )}
</Link>
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
