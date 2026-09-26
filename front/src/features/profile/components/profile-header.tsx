"use client";

import { useEffect, useState } from "react";
import type { PublicUserDto, UserDto } from "@/features/auth/types";
import { useAuth } from "@/features/auth/auth-context";
import { searchUsers, setFollowing } from "@/features/follows/api";
import FollowButton from "@/features/follows/components/follow-button";
import ProfileEditor from "./profile-editor";
import PasswordChangeDialog from "./password-change-dialog";
import styles from "./profile.module.css";

type Props = {
  user: UserDto | PublicUserDto;
  accessToken?: string;
  editable: boolean;
  onSaved?: (user: UserDto) => void;
};

export default function ProfileHeader({ user, accessToken, editable, onSaved }: Props) {
  const auth = useAuth();
  const [editing, setEditing] = useState(false);
  const [changingPassword, setChangingPassword] = useState(false);
  const [following, setFollowingState] = useState<boolean | null>(null);
  const [followPending, setFollowPending] = useState(false);

  async function handleLogout() {
    await auth.logout();
    window.location.href = "/";
  }

  useEffect(() => {
    if (editable || !accessToken) { setFollowingState(null); return; }
    const controller = new AbortController();
    // 유저 개별 팔로우 상태 조회 API가 아직 없어서, 닉네임 검색 결과에서 정확히 일치하는 항목을 찾아 상태를 읽어온다.
    searchUsers(user.nickname, 0, accessToken, controller.signal, 5)
      .then((response) => {
        const match = response.users.find((row) => row.nickname === user.nickname);
        if (!controller.signal.aborted) setFollowingState(match?.followedByMe ?? false);
      })
      .catch(() => { if (!controller.signal.aborted) setFollowingState(false); });
    return () => controller.abort();
  }, [editable, accessToken, user.nickname]);

  async function toggleFollow() {
    if (!accessToken || following === null || followPending) return;
    setFollowPending(true);
    try {
      const result = await setFollowing(user.id, !following, accessToken);
      setFollowingState(result.followed);
    } catch {
      // 실패해도 상태는 그대로 유지, 다시 눌러서 재시도 가능
    } finally {
      setFollowPending(false);
    }
  }

  return <section className={styles.profileHeader} aria-label={editable ? "내 프로필" : `${user.nickname}님의 프로필`}>
    {user.profileImageUrl
      ? /* eslint-disable-next-line @next/next/no-img-element */
        <img className={styles.avatar} src={user.profileImageUrl} alt={`${user.nickname} 프로필`} />
      : <div className={styles.avatarFallback}>{user.nickname.slice(0, 1).toUpperCase()}</div>}
    <div className={styles.profileIdentity}>
      <span className={styles.profileEyebrow}>{editable ? "MY GAME LOG" : "GAME LOG"}</span>
      <h1 className={styles.nickname}>{user.nickname}</h1>
      <p className={styles.profileBio}>{user.bio?.trim() || "아직 한줄 소개가 없어요."}</p>
      {editable && <div className={styles.profileHeaderActions}>
        <button type="button" className={styles.profileEditButton} onClick={() => setEditing(true)}>프로필 수정</button>
        <button type="button" className={styles.profileEditButton} onClick={() => setChangingPassword(true)}>비밀번호 변경</button>
        <button type="button" className={`${styles.profileEditButton} ${styles.profileLogoutButton}`} onClick={() => { void handleLogout(); }}>로그아웃</button>
      </div>}
      {!editable && accessToken && following !== null && <div className={styles.profileHeaderActions}>
        <FollowButton
          user={{ userId: user.id, nickname: user.nickname, profileImageUrl: user.profileImageUrl ?? null, followedAt: null, followedByMe: following, me: false }}
          pending={followPending}
          disabled={followPending}
          onClick={() => void toggleFollow()}
        />
      </div>}
    </div>
    {editing && <ProfileEditor user={user as UserDto} accessToken={accessToken ?? ""} onSaved={(updatedUser) => {
            onSaved?.(updatedUser);
            setEditing(false);
          }} onClose={() => setEditing(false)} />}
    {changingPassword && <PasswordChangeDialog accessToken={accessToken ?? ""} onClose={() => setChangingPassword(false)} />}
  </section>;
}
