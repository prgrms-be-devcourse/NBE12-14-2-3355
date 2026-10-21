import { Suspense } from "react";
import type { Metadata } from "next";
import Community from "@/features/community/community";

export const metadata: Metadata = { title: "커뮤니티 | GameLog" };

export default function CommunityPage() {
  return <Suspense fallback={<p role="status">커뮤니티를 불러오는 중…</p>}><Community /></Suspense>;
}
