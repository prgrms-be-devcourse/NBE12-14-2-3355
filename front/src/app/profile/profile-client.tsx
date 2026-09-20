"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/features/auth/auth-context";
import { checkNicknameDuplicate, updateProfile } from "@/features/auth/api";
import type { UserDto } from "@/features/auth/types";
import AuthNav from "@/components/auth/auth-nav";
import styles from "./profile-client.module.css";

type FieldStatus = "idle" | "checking" | "available" | "duplicate" | "error";

function ProfileSidebar({
  user,
  accessToken,
  onSaved,
}: {
  user: UserDto;
  accessToken: string;
  onSaved: (user: UserDto) => void;
}) {
  const [nickname, setNickname] = useState(user.nickname);
  const [bio, setBio] = useState("");
  const [profileImageUrl, setProfileImageUrl] = useState("");

  const [editingNickname, setEditingNickname] = useState(false);
  const [nicknameDraft, setNicknameDraft] = useState(user.nickname);
  const [nicknameStatus, setNicknameStatus] = useState<FieldStatus>("idle");

  const [editingBio, setEditingBio] = useState(false);
  const [bioDraft, setBioDraft] = useState("");

  const [editingImage, setEditingImage] = useState(false);
  const [imageDraft, setImageDraft] = useState("");

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  async function persist(next: { nickname: string; bio: string; profileImageUrl: string }) {
    setError("");
    setMessage("");
    setSaving(true);
    try {
      const updated = await updateProfile(
        { nickname: next.nickname, bio: next.bio || null, profileImageUrl: next.profileImageUrl || null },
        accessToken,
      );
      onSaved(updated);
      setMessage("저장했어요.");
      return true;
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "저장하지 못했어요.");
      return false;
    } finally {
      setSaving(false);
    }
  }

  function openNicknameEdit() {
    setNicknameDraft(nickname);
    setNicknameStatus("idle");
    setError("");
    setEditingNickname(true);
  }

  async function checkNickname() {
    const value = nicknameDraft.trim();
    if (!value) { setError("닉네임을 입력해 주세요."); return; }
    setError("");
    setNicknameStatus("checking");
    try {
      const duplicate = await checkNicknameDuplicate(value);
      setNicknameStatus(duplicate ? "duplicate" : "available");
    } catch {
      setNicknameStatus("error");
    }
  }

  async function saveNickname() {
    const value = nicknameDraft.trim();
    if (!value) { setError("닉네임을 입력해 주세요."); return; }
    const changed = value !== nickname;
    if (changed && nicknameStatus !== "available") { setError("변경한 닉네임은 중복확인을 먼저 해주세요."); return; }
    const ok = await persist({ nickname: value, bio, profileImageUrl });
    if (ok) { setNickname(value); setEditingNickname(false); }
  }

  function openBioEdit() { setBioDraft(bio); setError(""); setEditingBio(true); }
  async function saveBio() {
    const ok = await persist({ nickname, bio: bioDraft.trim(), profileImageUrl });
    if (ok) { setBio(bioDraft.trim()); setEditingBio(false); }
  }

  function openImageEdit() { setImageDraft(profileImageUrl); setError(""); setEditingImage(true); }
  async function saveImage() {
    const ok = await persist({ nickname, bio, profileImageUrl: imageDraft.trim() });
    if (ok) { setProfileImageUrl(imageDraft.trim()); setEditingImage(false); }
  }

  const anyEditing = editingNickname || editingBio || editingImage;

  return (
    <aside className={styles.sidebar}>
      <div className={styles.avatarBlock}>
        {profileImageUrl
          // eslint-disable-next-line @next/next/no-img-element
          ? <img className={styles.avatar} src={profileImageUrl} alt="" />
          : <div className={styles.avatarFallback}>{nickname.slice(0, 1).toUpperCase()}</div>}
        {editingImage ? (
          <div className={styles.inlineEdit}>
            <input value={imageDraft} onChange={(event) => setImageDraft(event.target.value)} placeholder="https://..." />
            <div className={styles.inlineActions}>
              <button type="button" onClick={() => setEditingImage(false)} disabled={saving}>취소</button>
              <button type="button" className={styles.primaryBtn} onClick={saveImage} disabled={saving}>저장</button>
            </div>
          </div>
        ) : (
          <button type="button" className={styles.linkBtn} onClick={openImageEdit}>이미지 변경</button>
        )}
      </div>

      <div className={styles.block}>
        {editingNickname ? (
          <div className={styles.inlineEdit}>
            <div className={styles.checkRow}>
              <input
                value={nicknameDraft}
                onChange={(event) => { setNicknameDraft(event.target.value); setNicknameStatus("idle"); }}
                autoComplete="nickname"
              />
              <button
                type="button"
                className={styles.checkButton}
                onClick={checkNickname}
                disabled={nicknameDraft.trim() === nickname || nicknameStatus === "checking"}
              >중복확인</button>
            </div>
            {nicknameDraft.trim() !== nickname && nicknameStatus === "checking" && <small className={styles.hint}>확인 중…</small>}
            {nicknameDraft.trim() !== nickname && nicknameStatus === "available" && <small className={styles.hintOk}>사용 가능한 닉네임입니다.</small>}
            {nicknameDraft.trim() !== nickname && nicknameStatus === "duplicate" && <small className={styles.hintError}>이미 사용 중인 닉네임입니다.</small>}
            <div className={styles.inlineActions}>
              <button type="button" onClick={() => setEditingNickname(false)} disabled={saving}>취소</button>
              <button type="button" className={styles.primaryBtn} onClick={saveNickname} disabled={saving}>저장</button>
            </div>
          </div>
        ) : (
          <h1 className={styles.nickname}>{nickname} <button type="button" className={styles.linkBtn} onClick={openNicknameEdit}>변경</button></h1>
        )}
        <span className={styles.email}>{user.email}</span>
      </div>

      <div className={styles.block}>
        <span className={styles.sectionLabel}>Bio</span>
        {editingBio ? (
          <div className={styles.inlineEdit}>
            <textarea rows={3} value={bioDraft} onChange={(event) => setBioDraft(event.target.value)} placeholder="나를 한 줄로 소개해 보세요" />
            <div className={styles.inlineActions}>
              <button type="button" onClick={() => setEditingBio(false)} disabled={saving}>취소</button>
              <button type="button" className={styles.primaryBtn} onClick={saveBio} disabled={saving}>저장</button>
            </div>
          </div>
        ) : bio ? (
          <p className={styles.bioText}>{bio} <button type="button" className={styles.linkBtn} onClick={openBioEdit}>수정</button></p>
        ) : (
          <p className={styles.bioEmpty}>아직 없어요 <button type="button" className={styles.linkBtn} onClick={openBioEdit}>+ 추가</button></p>
        )}
      </div>

      {error && <p className={styles.error} role="alert">{error}</p>}
      {message && !anyEditing && <p className={styles.success} role="status">{message}</p>}
      <p className={styles.note}>
        한줄소개·프로필 이미지는 서버가 아직 다시 불러오는 걸 지원하지 않아서, 새로고침하면 빈 칸으로 다시 보여요.
        이 화면을 벗어나지 않는 동안 순서대로 저장한 내용은 유지돼요.
      </p>
    </aside>
  );
}

export default function ProfileClient() {
  const router = useRouter();
  const auth = useAuth();

  useEffect(() => {
    if (auth.status === "unauthenticated") router.replace("/login?next=/profile");
  }, [auth.status, router]);

  return (
    <>
      <header className="header"><div className="header-inner">
        <Link className="logo" href="/" aria-label="GameLog 홈">GameLog<span className="lime">.</span></Link>
        <nav aria-label="주요 메뉴"><Link href="/">게임 탐색</Link></nav>
        <AuthNav />
        <span className="header-note">PLAY. RECORD. DISCOVER.</span>
      </div></header>

      <main className="main">
        {auth.status === "authenticated" && auth.user && auth.accessToken ? (
          <div className={styles.layout}>
            <ProfileSidebar key={auth.user.id} user={auth.user} accessToken={auth.accessToken} onSaved={auth.setUser} />
            <div className={styles.content}>
              <span className={styles.eyebrow}>MY GAME LOG</span>
              <h2 className={styles.contentTitle}>게임 기록 통계</h2>
              <p className={styles.contentBody}>준비 중이에요. 곧 여기서 플레이한 게임, 리뷰, 좋아요 같은 기록을 한눈에 볼 수 있게 될 거예요.</p>
            </div>
          </div>
        ) : (
          <p className={styles.loading}>{auth.status === "loading" ? "확인 중…" : "로그인 페이지로 이동 중…"}</p>
        )}
      </main>
    </>
  );
}
