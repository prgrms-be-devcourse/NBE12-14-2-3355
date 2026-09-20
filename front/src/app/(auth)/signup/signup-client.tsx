"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/features/auth/auth-context";
import { checkEmailDuplicate, checkNicknameDuplicate } from "@/features/auth/api";
import styles from "./signup-client.module.css";

type FieldStatus = "idle" | "checking" | "available" | "duplicate" | "error";

export default function SignupClient() {
  const router = useRouter();
  const auth = useAuth();
  const [nickname, setNickname] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [emailStatus, setEmailStatus] = useState<FieldStatus>("idle");
  const [nicknameStatus, setNicknameStatus] = useState<FieldStatus>("idle");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  async function checkEmail() {
    const value = email.trim();
    if (!value) { setError("이메일을 먼저 입력해 주세요."); return; }
    setError("");
    setEmailStatus("checking");
    try {
      const duplicate = await checkEmailDuplicate(value);
      setEmailStatus(duplicate ? "duplicate" : "available");
    } catch {
      setEmailStatus("error");
    }
  }

  async function checkNickname() {
    const value = nickname.trim();
    if (!value) { setError("닉네임을 먼저 입력해 주세요."); return; }
    setError("");
    setNicknameStatus("checking");
    try {
      const duplicate = await checkNicknameDuplicate(value);
      setNicknameStatus(duplicate ? "duplicate" : "available");
    } catch {
      setNicknameStatus("error");
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    if (!nickname.trim()) { setError("닉네임을 입력해 주세요."); return; }
    if (!email.trim()) { setError("이메일을 입력해 주세요."); return; }
    if (nicknameStatus !== "available") { setError("닉네임 중복확인을 먼저 해주세요."); return; }
    if (emailStatus !== "available") { setError("이메일 중복확인을 먼저 해주세요."); return; }
    if (password.length < 8) { setError("비밀번호는 8자 이상이어야 합니다."); return; }
    if (password !== passwordConfirm) { setError("비밀번호가 일치하지 않습니다."); return; }

    setSubmitting(true);
    try {
      await auth.signup({ nickname, email, password });
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "회원가입에 실패했습니다.");
      setSubmitting(false);
      return;
    }

    try {
      // 가입 직후 같은 자격 증명으로 바로 로그인해서 온보딩으로 이어간다(쿠팡/유튜브 뮤직류 패턴).
      await auth.login({ email, password });
      router.replace("/onboarding");
    } catch {
      // 계정 생성 자체는 성공했으니 에러로 겁주지 않고 로그인 페이지로 보내 다시 시도하게 한다.
      router.replace(`/login?email=${encodeURIComponent(email)}`);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className={styles.panel}>
      <span className={styles.eyebrow}>JOIN GAMELOG</span>
      <h1 className={styles.title}>회원가입</h1>
      <form className={styles.form} onSubmit={submit} noValidate>
        <label>닉네임
          <div className={styles.checkRow}>
            <input required value={nickname} onChange={(event) => { setNickname(event.target.value); setNicknameStatus("idle"); }} autoComplete="nickname" />
            <button type="button" className={styles.checkButton} onClick={checkNickname} disabled={nicknameStatus === "checking"}>중복확인</button>
          </div>
          {nicknameStatus === "checking" && <small className={styles.hint}>확인 중…</small>}
          {nicknameStatus === "available" && <small className={styles.hintOk}>사용 가능한 닉네임입니다.</small>}
          {nicknameStatus === "duplicate" && <small className={styles.hintError}>이미 사용 중인 닉네임입니다.</small>}
          {nicknameStatus === "error" && <small className={styles.hintError}>확인 중 오류가 발생했습니다. 다시 시도해 주세요.</small>}
        </label>
        <label>이메일
          <div className={styles.checkRow}>
            <input type="email" required value={email} onChange={(event) => { setEmail(event.target.value); setEmailStatus("idle"); }} autoComplete="email" />
            <button type="button" className={styles.checkButton} onClick={checkEmail} disabled={emailStatus === "checking"}>중복확인</button>
          </div>
          {emailStatus === "checking" && <small className={styles.hint}>확인 중…</small>}
          {emailStatus === "available" && <small className={styles.hintOk}>사용 가능한 이메일입니다.</small>}
          {emailStatus === "duplicate" && <small className={styles.hintError}>이미 사용 중인 이메일입니다.</small>}
          {emailStatus === "error" && <small className={styles.hintError}>확인 중 오류가 발생했습니다. 다시 시도해 주세요.</small>}
        </label>
        <label>비밀번호
          <input type="password" required minLength={8} value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="new-password" />
        </label>
        <label>비밀번호 확인
          <input type="password" required value={passwordConfirm} onChange={(event) => setPasswordConfirm(event.target.value)} autoComplete="new-password" />
        </label>
        {error && <p className={styles.error} role="alert">{error}</p>}
        <button type="submit" className={styles.submit} disabled={submitting}>{submitting ? "가입 중…" : "회원가입"}</button>
      </form>
      <p className={styles.footer}>이미 계정이 있으신가요? <Link href="/login">로그인</Link></p>
    </div>
  );
}
