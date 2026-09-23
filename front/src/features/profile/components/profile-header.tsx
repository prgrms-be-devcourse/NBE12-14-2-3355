"use client";

import { useState } from "react";
import type { PublicUserDto, UserDto } from "@/features/auth/types";
import { useAuth } from "@/features/auth/auth-context";
import ProfileEditor from "./profile-editor";
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
        <button type="button" className={`${styles.profileEditButton} ${styles.profileLogoutButton}`} onClick={() => { void auth.logout(); }}>로그아웃</button>
      </div>}
    </div>
    {editing && <ProfileEditor user={user as UserDto} accessToken={accessToken ?? ""} onSaved={(updatedUser) => {
            onSaved?.(updatedUser);
            setEditing(false);
          }} onClose={() => setEditing(false)} />}
  </section>;
}
