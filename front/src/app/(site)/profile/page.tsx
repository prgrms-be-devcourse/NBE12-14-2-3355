import type { Metadata } from "next";
import ProfileScreen from "@/features/profile/components/profile-page";

export const metadata: Metadata = { title: "프로필 | GameLog" };

// 로그인 사용자의 기본 프로필 화면
export default function ProfilePage() {
  return <ProfileScreen />;
}
