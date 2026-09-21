import type { Metadata } from "next";
import OnboardingClient from "./onboarding-client";

export const metadata: Metadata = { title: "온보딩 | GameLog" };

export default function OnboardingPage() {
  return <OnboardingClient />;
}
