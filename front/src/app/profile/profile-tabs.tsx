import Link from "next/link";
import styles from "./profile-client.module.css";

export type ProfileSection = "profile" | "games" | "reviews" | "likes" | "following" | "followers";

export default function ProfileTabs({ section }: { section: ProfileSection }) {
  const tabs = [
    { label: "Profile", href: "/profile", active: section === "profile" },
    { label: "Games", href: "/profile/games", active: section === "games" },
    { label: "Reviews", href: "/profile/reviews", active: section === "reviews" },
    { label: "Friends", href: "/profile/following", active: section === "following" || section === "followers" },
    { label: "Likes", href: "/profile/likes", active: section === "likes" },
  ];
  return <nav className={styles.profileTabs} aria-label="프로필 메뉴">
    {tabs.map(tab => <Link key={tab.label} href={tab.href} aria-current={tab.active ? "page" : undefined}>{tab.label}</Link>)}
  </nav>;
}
