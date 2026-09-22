"use client";

import { useEffect, useRef, useState } from "react";
import { checkNicknameDuplicate, updateProfile, uploadProfileImage } from "@/features/auth/api";
import type { UserDto } from "@/features/auth/types";
import styles from "./profile.module.css";

// 로그인한 사용자의 프로필 수정 다이얼로그
export default function ProfileEditor({ user, accessToken, onSaved, onClose }: {
  user: UserDto; accessToken: string; onSaved: (user: UserDto) => void; onClose: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const fileInput = useRef<HTMLInputElement>(null);
  const [nickname, setNickname] = useState(user.nickname);
  const [bio, setBio] = useState(user.bio ?? "");
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState(user.profileImageUrl ?? "");
  const [removeImage, setRemoveImage] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => { dialog.current?.showModal(); }, []);
  useEffect(() => {
    if (!preview.startsWith("blob:")) return;
    return () => URL.revokeObjectURL(preview);
  }, [preview]);

  return <dialog ref={dialog} className={styles.editorDialog} aria-labelledby="profile-editor-title" onCancel={event => {
    // File inputs also emit cancel; only the dialog's own Escape event should close it.
    if (event.target !== event.currentTarget) return;
    event.preventDefault();
    if (!saving) onClose();
  }}>
    <form className={styles.editorForm} onSubmit={async event => {
      event.preventDefault();
      const value = nickname.trim();
      if (!value) { setError("닉네임을 입력해 주세요."); return; }
      setSaving(true); setError("");
      try {
        if (value !== user.nickname && await checkNicknameDuplicate(value)) throw new Error("이미 사용 중인 닉네임입니다.");
        const profileImageUrl = file ? await uploadProfileImage(file, accessToken) : removeImage ? null : user.profileImageUrl;
        const updated = await updateProfile({ nickname: value, bio: bio.trim() || null, profileImageUrl: profileImageUrl || null }, accessToken);
        onSaved(updated); onClose();
      } catch (reason) { setError(reason instanceof Error ? reason.message : "프로필 저장에 실패했어요."); }
      finally { setSaving(false); }
    }}>
      <h2 id="profile-editor-title">프로필 수정</h2>
      <fieldset disabled={saving} className={styles.editorFields}>
        <div className={styles.editorAvatarRow}>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          {preview ? <img src={preview} alt="프로필 이미지 미리보기" className={styles.avatar} /> : <div className={styles.avatarFallback}>{nickname.slice(0, 1)}</div>}
          <input ref={fileInput} type="file" accept="image/*" className={styles.hiddenFileInput} onChange={event => {
            const selected = event.target.files?.[0];
            if (selected && !selected.type.startsWith("image/")) { setError("이미지 파일을 선택해 주세요."); return; }
            if (selected) { setFile(selected); setPreview(URL.createObjectURL(selected)); setRemoveImage(false); setError(""); }
          }} />
          <div className={styles.editorAvatarActions}>
            <button type="button" className={styles.linkBtn} onClick={() => fileInput.current?.click()}>업로드</button>
            {preview && (
              <button
                type="button"
                className={styles.iconBtn}
                title="사진 삭제"
                aria-label="사진 삭제"
                onClick={() => { setFile(null); setPreview(""); setRemoveImage(true); setError(""); }}
              >
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
                  <path d="M10 11v6" />
                  <path d="M14 11v6" />
                  <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
                </svg>
              </button>
            )}
          </div>
        </div>
        <label>닉네임<input autoComplete="nickname" required value={nickname} onChange={event => setNickname(event.target.value)} /></label>
        <label>한줄 소개<input value={bio} onChange={event => setBio(event.target.value)} placeholder="나를 한 줄로 소개해 보세요" /></label>
      </fieldset>
      {error && <p role="alert" className={styles.error}>{error}</p>}
      <div className={styles.inlineActions}><button type="button" onClick={onClose} disabled={saving}>취소</button><button type="submit" className={styles.primaryBtn} disabled={saving}>{saving ? "저장 중…" : "저장"}</button></div>
    </form>
  </dialog>;
}
