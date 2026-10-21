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
  const [done, setDone] = useState(false);
  useEffect(() => { dialog.current?.showModal(); }, []);

  return <dialog ref={dialog} className={styles.editorDialog} aria-labelledby="password-change-title" onCancel={event => {
    if (event.target !== event.currentTarget) return;
    event.preventDefault();
    if (!saving) onClose();
  }}>
    {done ? (
      // 백엔드가 다른 기기의 세션은 폐기하고, 현재 기기는 새 refreshToken 쿠키로 교체해 주므로 로그인 상태가 유지된다.
      <div className={styles.editorForm}>
        <h2 id="password-change-title">비밀번호 변경 완료</h2>
        <p role="status">비밀번호가 변경되었습니다. 다른 기기에서는 로그아웃되었습니다.</p>
        <div className={styles.inlineActions}>
          <button type="button" className={styles.primaryBtn} onClick={onClose}>확인</button>
        </div>
      </div>
    ) : (
      <form className={styles.editorForm} onSubmit={async event => {
        event.preventDefault();
        setError("");
        if (newPassword.length < 8) { setError("새 비밀번호는 8자 이상이어야 합니다."); return; }
        if (newPassword !== newPasswordConfirm) { setError("새 비밀번호가 서로 일치하지 않습니다."); return; }
        setSaving(true);
        try {
          await changePassword({ currentPassword, newPassword }, accessToken);
          setDone(true);
        } catch (reason) {
          setError(reason instanceof AuthApiError ? reason.message : "비밀번호 변경에 실패했습니다.");
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
    )}
  </dialog>;
}
