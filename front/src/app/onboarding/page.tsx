import type { Metadata } from "next";
import { Suspense } from "react";
import OnboardingClient from "./onboarding-client";

export const metadata: Metadata = { title: "온보딩 | GameLog" };

export default function OnboardingPage() {
  return <Suspense fallback={<p role="status">온보딩을 불러오는 중…</p>}><OnboardingClient /></Suspense>;
}
