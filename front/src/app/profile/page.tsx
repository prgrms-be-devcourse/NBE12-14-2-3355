import type { Metadata } from "next";
import ProfileClient from "./profile-client";

export const metadata: Metadata = { title: "프로필 | GameLog" };

export default function ProfilePage() {
  return <ProfileClient />;
}
