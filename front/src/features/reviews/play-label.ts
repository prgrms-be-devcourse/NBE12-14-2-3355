import type { Review } from "./list-types";

export function getPlayLabel(review: Pick<Review, "playStatus" | "playing" | "backlog" | "wishlist">) {
  if (review.playing) return "플레이 중";
  if (review.playStatus === "COMPLETED" || review.playStatus === "PLAYED") return "플레이 완료";
  if (review.playStatus === "RETIRED" || review.playStatus === "DROPPED") return "플레이 중단";
  if (review.playStatus === "SHELVED") return "보류";
  if (review.backlog) return "플레이 예정";
  if (review.wishlist) return "위시리스트";
  return null;
}
