import GameDetailView from "@/components/game-detail-view";
import { notFound } from "next/navigation";

export default async function GameDetailPage({ params }: { params: Promise<{ gameId: string }> }) {
  const { gameId } = await params;
  if (!/^\d+$/.test(gameId)) notFound();

  return <GameDetailView key={gameId} gameId={gameId} />;
}
