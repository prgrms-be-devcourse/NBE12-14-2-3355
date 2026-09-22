import type { Metadata } from "next";
import GameCatalog from "@/features/games/components/game-catalog";

export const metadata: Metadata = { title: "전체 게임 | GameLog" };

// 검색·필터를 제공하는 전체 게임 화면
export default async function GamesPage({ searchParams }: {
  searchParams: Promise<{ keyword?: string | string[] }>;
}) {
  const { keyword } = await searchParams;
  const initialKeyword = typeof keyword === "string" ? keyword : "";
  return <GameCatalog key={initialKeyword} initialKeyword={initialKeyword} />;
}
