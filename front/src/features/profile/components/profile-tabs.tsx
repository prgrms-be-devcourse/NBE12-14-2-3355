import Link from "next/link";
import styles from "./profile.module.css";

export type ProfileSection = "profile" | "games" | "reviews" | "likes" | "settings" | "following" | "followers";

type Props = {
  section: ProfileSection;
  userId?: number;
  showSettings?: boolean;
};

// 프로필 하위 경로를 연결하는 공통 탭 (Settings는 내 프로필에서만)
export default function ProfileTabs({ section, userId, showSettings = false }: Props ) {
  const basePath =
    userId !== undefined
      ? `/profile/${userId}`
      : "/profile";

  const tabs = [
    { label: "Profile", href: basePath, active: section === "profile" },
    { label: "Games", href: `${basePath}/games`, active: section === "games" },
    { label: "Reviews", href: `${basePath}/reviews`, active: section === "reviews" },
    { label: "Friends", href: `${basePath}/following`, active: section === "following" || section === "followers" },
    { label: "Likes", href: `${basePath}/likes`, active: section === "likes" },
    ...(showSettings ? [{ label: "Settings", href: "/profile/settings", active: section === "settings" }] : []),
  ];
  return <nav className={styles.profileTabs} aria-label="프로필 메뉴">
    {tabs.map(tab => <Link key={tab.label} href={tab.href} aria-current={tab.active ? "page" : undefined}>{tab.label}</Link>)}
  </nav>;
}
