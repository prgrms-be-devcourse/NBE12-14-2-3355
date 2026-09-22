import { acceptRefreshedToken, AuthApiError } from "@/features/auth/api";
import type { Option } from "@/lib/games";

export type PersonalizedGame = {
  id: number;
  title: string;
  coverImageUrl: string | null;
  igdbRating: number | null;
  recommendationScore: number;
  genres: Option[];
};

// 서버에서 계산한 추천 순서를 유지해 최대 5개 조회
export async function getPersonalizedGames(accessToken: string, signal: AbortSignal): Promise<PersonalizedGame[]> {
  const response = await fetch("/api/games/recommendations/personalized", {
    headers: { Authorization: `Bearer ${accessToken}` },
    cache: "no-store",
    signal,
  });
  const body = await response.json();
  signal.throwIfAborted();
  if (!response.ok) throw new AuthApiError(body.msg || "추천 조회 실패", response.status);
  if (!Array.isArray(body.data)) throw new Error("추천 응답 형식 오류");
  acceptRefreshedToken(response);
  return body.data.slice(0, 5);
}
