import type { Metadata } from "next";
import { notFound, redirect } from "next/navigation";
import ProfileClient from "../profile-client";
import PublicFriendsClient from "../public-friends-client";

export const metadata: Metadata = { title: "프로필 | GameLog" };

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
    return <ProfileClient section={first} page={page} />;
  }
  if (segments.length === 2 && /^[1-9]\d*$/.test(first) && Number.isSafeInteger(Number(first)) && (second === "following" || second === "followers")) {
    return <PublicFriendsClient userId={Number(first)} kind={second} page={page} />;
  }
  notFound();
}
