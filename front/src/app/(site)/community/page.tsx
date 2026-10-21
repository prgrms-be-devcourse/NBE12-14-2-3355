import { Suspense } from "react";
import type { Metadata } from "next";
import Community from "@/features/community/community";

export const metadata: Metadata = { title: "피드 | GameLog" };

export default function CommunityPage() {
  return <Suspense fallback={<p role="status">피드를 불러오는 중…</p>}><Community /></Suspense>;
}
