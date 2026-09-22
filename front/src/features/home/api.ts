export type PopularGenre = {
  id: number;
  name: string;
};

export type PopularGame = {
  gameId: number;
  title: string;
  coverImageUrl: string | null;
  likeCount: number;
  genres: PopularGenre[];
};

export type PopularReview = {
  reviewId: number;
  userId: number;
  nickname: string | null;
  profileImageUrl: string | null;
  gameId: number;
  gameTitle: string;
  gameCoverImageUrl: string | null;
  rating: number | null;
  content: string;
  spoiler: boolean;
  likeCount: number;
  createdDate: string;
};

type ApiResponse<T> = {
  data?: T;
  msg?: string;
};

async function getPopularContent<T>(path: string, signal: AbortSignal): Promise<T[]> {
  const response = await fetch(path, { cache: "no-store", signal });
  const body = (await response.json()) as ApiResponse<T[]>;
  signal.throwIfAborted();

  if (!response.ok) throw new Error(body.msg || "인기 콘텐츠를 불러오지 못했어요.");
  if (!Array.isArray(body.data)) throw new Error("인기 콘텐츠 응답 형식이 올바르지 않아요.");

  return body.data;
}

export function getPopularGames(signal: AbortSignal) {
  return getPopularContent<PopularGame>("/api/games/popular?size=5", signal);
}

export function getPopularReviews(signal: AbortSignal) {
  return getPopularContent<PopularReview>("/api/review-api/reviews/popular?size=5", signal);
}
