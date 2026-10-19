"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { AuthApiError, changePassword } from "@/features/auth/api";
import { useAuth } from "@/features/auth/auth-context";
import { LOGIN_NOTICE_KEY } from "@/features/auth/login-notice";
import styles from "./profile.module.css";

// 로그인한 사용자의 비밀번호 변경 다이얼로그
export default function PasswordChangeDialog({ accessToken, onClose }: {
  accessToken: string; onClose: () => void;
}) {
  const auth = useAuth();
  const router = useRouter();
  const dialog = useRef<HTMLDialogElement>(null);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => { dialog.current?.showModal(); }, []);

  return <dialog ref={dialog} className={styles.editorDialog} aria-labelledby="password-change-title" onCancel={event => {
    if (event.target !== event.currentTarget) return;
    event.preventDefault();
    if (!saving) onClose();
  }}>
    <form className={styles.editorForm} onSubmit={async event => {
      event.preventDefault();
      setError("");
      if (newPassword.length < 8) { setError("새 비밀번호는 8자 이상이어야 합니다."); return; }
      if (newPassword !== newPasswordConfirm) { setError("새 비밀번호가 서로 일치하지 않습니다."); return; }
      setSaving(true);
      try {
        await changePassword({ currentPassword, newPassword }, accessToken);
      } catch (reason) {
        setError(reason instanceof AuthApiError ? reason.message : "비밀번호 변경에 실패했습니다.");
        setSaving(false);
        return;
      }
      // 백엔드가 모든 기기의 refreshToken을 폐기하므로 로그인 페이지에서 재로그인 안내를 띄운다.
      // logout 직후 프로필 페이지 가드가 /login?next=... 로 먼저 이동시켜 쿼리가 덮이므로 sessionStorage로 전달한다.
      try {
        sessionStorage.setItem(LOGIN_NOTICE_KEY, JSON.stringify({ reason: "password-changed", email: auth.user?.email ?? "" }));
      } catch {
        // 저장소를 못 쓰는 환경이면 안내 없이 로그인 페이지로만 이동한다.
      }
      await auth.logout();
      router.replace("/login");
    }}>
      <h2 id="password-change-title">비밀번호 변경</h2>
      <fieldset disabled={saving} className={styles.editorFields}>
        <label>현재 비밀번호
          <input type="password" required autoComplete="current-password" value={currentPassword} onChange={event => setCurrentPassword(event.target.value)} />
        </label>
        <label>새 비밀번호
          <input type="password" required autoComplete="new-password" value={newPassword} onChange={event => setNewPassword(event.target.value)} />
        </label>
        <label>새 비밀번호 확인
          <input type="password" required autoComplete="new-password" value={newPasswordConfirm} onChange={event => setNewPasswordConfirm(event.target.value)} />
        </label>
      </fieldset>
      {error && <p role="alert" className={styles.error}>{error}</p>}
      <div className={styles.inlineActions}>
        <button type="button" onClick={onClose} disabled={saving}>취소</button>
        <button type="submit" className={styles.primaryBtn} disabled={saving}>{saving ? "변경 중…" : "변경"}</button>
      </div>
    </form>
  </dialog>;
}
