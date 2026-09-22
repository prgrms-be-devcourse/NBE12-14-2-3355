import GameDetailView from "@/features/games/components/game-detail-view";
import { notFound } from "next/navigation";

// GameLog DB ID 기반 게임 상세 화면
export default async function GameDetailPage({ params }: { params: Promise<{ gameId: string }> }) {
  const { gameId } = await params;
  if (!/^\d+$/.test(gameId)) notFound();

  return <GameDetailView key={gameId} gameId={gameId} />;
}
