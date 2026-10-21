"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState, type ReactNode } from "react";
import { useAuth } from "@/features/auth/auth-context";
import styles from "./layout.module.css";

const menus = [
  { href: "/admin/reports", label: "신고 관리" },
  { href: "/admin/igdb", label: "게임 데이터 관리" },
];

export default function AdminLayout({ children }: { children: ReactNode }) {
  const auth = useAuth();
  const pathname = usePathname();
  const router = useRouter();
  const [loggingOut, setLoggingOut] = useState(false);
  const isAdmin = auth.status === "authenticated" && auth.user?.role === "ADMIN";

  async function logout() {
    if (loggingOut) return;
    setLoggingOut(true);
    try {
      await auth.logout();
      router.replace("/login");
    } finally {
      setLoggingOut(false);
    }
  }

  return (
    <div className={styles.shell}>
      {isAdmin && (
        <header className={styles.header}>
          <div className={styles.inner}>
            <Link href="/admin/reports" className={styles.brand} aria-label="GameLog 관리자 신고 관리">
              GameLog <span>관리자</span>
            </Link>
            <nav className={styles.nav} aria-label="관리자 메뉴">
              {menus.map(({ href, label }) => (
                <Link key={href} href={href} aria-current={pathname === href ? "page" : undefined}>
                  {label}
                </Link>
              ))}
            </nav>
            <button type="button" className={styles.logout} onClick={logout} disabled={loggingOut}>
              {loggingOut ? "로그아웃 중…" : "로그아웃"}
            </button>
          </div>
        </header>
      )}
      {children}
    </div>
  );
}
