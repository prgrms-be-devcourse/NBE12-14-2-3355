import type { Metadata } from "next";
import { notFound, redirect } from "next/navigation";
import ProfilePage from "@/features/profile/components/profile-page";
import PublicFriendsPage from "@/features/profile/components/public-friends-page";

export const metadata: Metadata = { title: "프로필 | GameLog" };

// 프로필 탭과 다른 사용자의 Friends 경로를 해석하는 페이지
export default async function ProfileSectionPage({ params, searchParams }: {
  params: Promise<{ segments: string[] }>;
  searchParams: Promise<{ page?: string | string[] }>;
}) {
  const { segments } = await params;
  const { page: rawPage } = await searchParams;
  const validPage = rawPage === undefined || (typeof rawPage === "string" && /^(0|[1-9]\d*)$/.test(rawPage) && Number(rawPage) <= 2147483647);
  if (!validPage) redirect(`/profile/${segments.map(encodeURIComponent).join("/")}`);
  const page = Number(rawPage ?? 0);
  const [first, second] = segments;

  if (segments.length === 1 && (first === "following" || first === "followers" || first === "games" || first === "reviews" || first === "likes")) {
    return <ProfilePage section={first} page={page} />;
  }
  if (segments.length === 2 && /^[1-9]\d*$/.test(first) && Number.isSafeInteger(Number(first)) && (second === "following" || second === "followers")) {
    return <PublicFriendsPage userId={Number(first)} kind={second} page={page} />;
  }
  notFound();
}
