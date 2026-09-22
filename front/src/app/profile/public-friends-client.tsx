"use client";

import Link from "next/link";
import AuthNav from "@/components/auth/auth-nav";
import { useAuth } from "@/features/auth/auth-context";
import type { FollowKind } from "@/features/follows/types";
import ProfileClient from "./profile-client";
import FollowList from "./follow-list";
import profileStyles from "./profile-client.module.css";
import styles from "./follow-list.module.css";

export default function PublicFriendsClient({ userId, kind, page }: { userId: number; kind: FollowKind; page: number }) {
  const auth = useAuth();
  if (auth.user?.id === userId) return <ProfileClient section={kind} page={page} />;

  return <>
    <header className="header"><div className="header-inner">
      <Link className="logo" href="/" aria-label="GameLog 홈">GameLog<span className="lime">.</span></Link>
      <nav aria-label="주요 메뉴"><Link href="/">게임 탐색</Link></nav>
      <AuthNav />
      <span className="header-note">PLAY. RECORD. DISCOVER.</span>
    </div></header>
    <main className="main"><div className={profileStyles.layout}>
      <div className={styles.publicHeading}><h1>Friends</h1><p>사용자 #{userId}의 팔로우 목록</p></div>
      <nav className={profileStyles.profileTabs} aria-label="사용자 메뉴">
        <Link href={`/profile/${userId}/following`} aria-current="page">Friends</Link>
      </nav>
      <FollowList userId={userId} kind={kind} page={page} basePath={`/profile/${userId}`} />
    </div></main>
  </>;
}
