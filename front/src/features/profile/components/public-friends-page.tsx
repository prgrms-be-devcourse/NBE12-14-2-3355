"use client";

import Link from "next/link";
import { useAuth } from "@/features/auth/auth-context";
import type { FollowKind } from "@/features/follows/types";
import FollowList from "@/features/follows/components/follow-list";
import ProfileLayout from "./profile-layout";
import ProfilePage from "./profile-page";
import profileStyles from "./profile.module.css";
import followStyles from "@/features/follows/components/follow-list.module.css";

export default function PublicFriendsPage({ userId, kind, page }: { userId: number; kind: FollowKind; page: number }) {
  const auth = useAuth();
  if (auth.user?.id === userId) return <ProfilePage section={kind} page={page} />;

  return <ProfileLayout>
      <div className={followStyles.publicHeading}><h1>Friends</h1><p>사용자 #{userId}의 팔로우 목록</p></div>
      <nav className={profileStyles.profileTabs} aria-label="사용자 메뉴">
        <Link href={`/profile/${userId}/following`} aria-current="page">Friends</Link>
      </nav>
      <FollowList userId={userId} kind={kind} page={page} basePath={`/profile/${userId}`} />
  </ProfileLayout>;
}
