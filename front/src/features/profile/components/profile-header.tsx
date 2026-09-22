"use client";

import { useState } from "react";
import type { UserDto } from "@/features/auth/types";
import ProfileEditor from "./profile-editor";
import styles from "./profile.module.css";

type Props = {
  user: UserDto;
  accessToken: string;
  onSaved: (user: UserDto) => void;
};

export default function ProfileHeader({ user, accessToken, onSaved }: Props) {
  const [editing, setEditing] = useState(false);

  return <section className={styles.profileHeader} aria-label="내 프로필">
    {user.profileImageUrl
      ? /* eslint-disable-next-line @next/next/no-img-element */
        <img className={styles.avatar} src={user.profileImageUrl} alt={`${user.nickname} 프로필`} />
      : <div className={styles.avatarFallback}>{user.nickname.slice(0, 1).toUpperCase()}</div>}
    <div className={styles.profileIdentity}>
      <span className={styles.profileEyebrow}>MY GAME LOG</span>
      <h1 className={styles.nickname}>{user.nickname}</h1>
      <p className={styles.profileBio}>{user.bio?.trim() || "아직 한줄 소개가 없어요."}</p>
      <button type="button" className={styles.profileEditButton} onClick={() => setEditing(true)}>프로필 수정</button>
    </div>
    {editing && <ProfileEditor user={user} accessToken={accessToken} onSaved={onSaved} onClose={() => setEditing(false)} />}
  </section>;
}
