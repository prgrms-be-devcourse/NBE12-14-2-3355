"use client";

import { useEffect, useRef, useState } from "react";
import { AuthApiError, withdraw } from "@/features/auth/api";
import { useAuth } from "@/features/auth/auth-context";
import styles from "./profile.module.css";

// 회원탈퇴 다이얼로그 (비밀번호 재확인 후 탈퇴, 완료 안내 뒤 로그아웃)
export default function WithdrawDialog({ accessToken, onClose }: {
  accessToken: string; onClose: () => void;
}) {
  const auth = useAuth();
  const dialog = useRef<HTMLDialogElement>(null);
  const [password, setPassword] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);
  useEffect(() => { dialog.current?.showModal(); }, []);

  // 탈퇴 후에는 세션이 모두 폐기되므로 프론트 상태도 로그아웃으로 정리하고 홈으로 이동한다.
  async function finish() {
    await auth.logout();
    window.location.href = "/";
  }

  return <dialog ref={dialog} className={styles.editorDialog} aria-labelledby="withdraw-title" onCancel={event => {
    if (event.target !== event.currentTarget) return;
    event.preventDefault();
    if (done) { void finish(); return; }
    if (!saving) onClose();
  }}>
    {done ? (
      <div className={styles.editorForm}>
        <h2 id="withdraw-title">회원탈퇴 완료</h2>
        <p role="status">탈퇴가 완료되었습니다. 그동안 이용해 주셔서 감사합니다.</p>
        <div className={styles.inlineActions}>
          <button type="button" className={styles.primaryBtn} onClick={() => { void finish(); }}>확인</button>
        </div>
      </div>
    ) : (
      <form className={styles.editorForm} onSubmit={async event => {
        event.preventDefault();
        setError("");
        if (!password) { setError("비밀번호를 입력해 주세요."); return; }
        setSaving(true);
        try {
          await withdraw({ password }, accessToken);
          setDone(true);
        } catch (reason) {
          setError(reason instanceof AuthApiError ? reason.message : "회원탈퇴에 실패했습니다.");
        } finally {
          setSaving(false);
        }
      }}>
        <h2 id="withdraw-title">회원탈퇴</h2>
        <p>탈퇴하면 작성한 리뷰와 기록은 복구할 수 없으며, 같은 이메일로 다시 가입해도 이전 기록은 연결되지 않습니다.</p>
        <fieldset disabled={saving} className={styles.editorFields}>
          <label>비밀번호 확인
            <input type="password" required autoComplete="current-password" value={password} onChange={event => setPassword(event.target.value)} />
          </label>
        </fieldset>
        {error && <p role="alert" className={styles.error}>{error}</p>}
        <div className={styles.inlineActions}>
          <button type="button" onClick={onClose} disabled={saving}>취소</button>
          <button type="submit" className={`${styles.primaryBtn} ${styles.profileLogoutButton}`} disabled={saving}>{saving ? "탈퇴 중…" : "탈퇴하기"}</button>
        </div>
      </form>
    )}
  </dialog>;
}
