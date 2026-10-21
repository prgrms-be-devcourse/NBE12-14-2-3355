import type { PopularReview } from "@/features/home/api";
import { ReviewApiError } from "@/features/reviews/api";

export type CommunityReview = PopularReview & {
  liked: boolean;
  playStatus: string | null;
  playing: boolean;
  backlog: boolean;
  wishlist: boolean;
};
export type Feed = { reviews: CommunityReview[]; hasNext: boolean; hasFollowing: boolean };
export type FeedTab = "following" | "popular";

export async function getFeed(tab: FeedTab, page: number, token: string, signal: AbortSignal): Promise<Feed> {
  const response = await fetch(`/api/review-api/community/reviews/${tab}?page=${page}`, {
    headers: { Authorization: `Bearer ${token}` }, cache: "no-store", signal,
  });
  const body = await response.json();
  if (!response.ok) throw new ReviewApiError(body.msg || "리뷰를 불러오지 못했습니다.", response.status);
  return body.data;
}
