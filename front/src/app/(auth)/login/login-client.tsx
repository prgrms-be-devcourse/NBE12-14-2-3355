"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/features/auth/auth-context";
import { AuthApiError } from "@/features/auth/api";
import { needsOnboarding } from "@/features/auth/onboarding-status";
import styles from "./login-client.module.css";

export default function LoginClient() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const auth = useAuth();
  const next = searchParams.get("next");
  const [email, setEmail] = useState(searchParams.get("email") ?? "");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (auth.status === "authenticated") {
      router.replace(auth.user?.role === "ADMIN" ? "/admin/reports" : next || "/");
    }
  }, [auth.status, auth.user, next, router]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    if (!email.trim()) { setError("이메일을 입력해 주세요."); return; }
    if (!password) { setError("비밀번호를 입력해 주세요."); return; }
    setSubmitting(true);
    try {
      const user = await auth.login({ email, password });
      router.replace(
        user.role === "ADMIN" ? "/admin/reports" : needsOnboarding(user) ? "/onboarding" : next || "/"
      );
    } catch (reason) {
      setError(reason instanceof AuthApiError && reason.status === 401
        ? "이메일 또는 비밀번호가 올바르지 않습니다."
        : reason instanceof Error ? reason.message : "로그인에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className={styles.panel}>
      <span className={styles.eyebrow}>WELCOME BACK</span>
      <h1 className={styles.title}>로그인</h1>
      <form className={styles.form} onSubmit={submit} noValidate>
        <label>이메일
          <input type="email" required value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" />
        </label>
        <label>비밀번호
          <input type="password" required value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" />
        </label>
        {error && <p className={styles.error} role="alert">{error}</p>}
        <button type="submit" className={styles.submit} disabled={submitting}>{submitting ? "로그인 중…" : "로그인"}</button>
      </form>
      <p className={styles.footer}>아직 계정이 없으신가요? <Link href="/signup">회원가입</Link></p>
    </div>
  );
}
