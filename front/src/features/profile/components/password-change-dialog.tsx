"use client";

import { useEffect, useRef, useState } from "react";
import { AuthApiError, changePassword } from "@/features/auth/api";
import styles from "./profile.module.css";

// 로그인한 사용자의 비밀번호 변경 다이얼로그
export default function PasswordChangeDialog({ accessToken, onClose }: {
  accessToken: string; onClose: () => void;
}) {
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
      if (newPassword.length < 8) { setError("새 비밀번호는 8자 이상이어야 해요."); return; }
      if (newPassword !== newPasswordConfirm) { setError("새 비밀번호가 서로 일치하지 않아요."); return; }
      setSaving(true);
      try {
        await changePassword({ currentPassword, newPassword }, accessToken);
        onClose();
      } catch (reason) {
        setError(reason instanceof AuthApiError ? reason.message : "비밀번호 변경에 실패했어요.");
      } finally {
        setSaving(false);
      }
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
