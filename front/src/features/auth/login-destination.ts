import { needsOnboarding } from "./onboarding-status";
import type { UserDto } from "./types";

export function safeReturnPath(value: string | null): string {
  if (!value || !value.startsWith("/") || value.startsWith("//") || /[\\\u0000-\u001f]/.test(value)) return "/";
  return value;
}

export function communityReturnPath(value: string | null): string | null {
  const safe = safeReturnPath(value);
  const url = new URL(safe, "https://gamelog.local");
  if (url.pathname !== "/community") return null;
  const query = new URLSearchParams();
  if (url.searchParams.get("tab") === "popular") query.set("tab", "popular");
  const review = url.searchParams.get("commentReview");
  if (review && /^[1-9]\d*$/.test(review)) query.set("commentReview", review);
  return `/community${query.size ? `?${query}` : ""}`;
}

export function reviewReturnPath(value: string | null): string | null {
  const community = communityReturnPath(value);
  if (community) return community;
  const url = new URL(safeReturnPath(value), "https://gamelog.local");
  const review = url.searchParams.get("commentReview");
  return /^\/games\/[1-9]\d*$/.test(url.pathname) && review && /^[1-9]\d*$/.test(review)
    ? `${url.pathname}?commentReview=${review}` : null;
}

export function loginDestination(user: UserDto, next: string | null, afterSubmit = true): string {
  const community = reviewReturnPath(next);
  if (community) {
    return user.role !== "ADMIN" && needsOnboarding(user)
      ? `/onboarding?next=${encodeURIComponent(community)}`
      : community;
  }
  if (user.role === "ADMIN") return "/admin/reports";
  if (afterSubmit && needsOnboarding(user)) return "/onboarding";
  return safeReturnPath(next);
}
