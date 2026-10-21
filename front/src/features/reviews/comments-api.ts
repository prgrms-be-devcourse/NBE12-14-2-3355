import { acceptRefreshedToken } from "@/features/auth/api";
import { ReviewApiError } from "./api";

export type Comment = {
  id: number; parentId: number | null; userId: number | null; nickname: string | null;
  profileImageUrl: string | null; content: string; spoiler: boolean; deleted: boolean;
  edited: boolean; author: boolean; mine: boolean; likeCount: number; liked: boolean;
  replyCount: number; createdDate: string;
};
export type CommentPage = { comments: Comment[]; nextCursor: number | null; totalCount: number };
export async function commentRequest<T>(reviewId: number, suffix = "", token?: string | null, method = "GET", body?: unknown, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`/api/review-api/reviews/${reviewId}/comments${suffix}`, {
    method, cache: "no-store", signal,
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(body ? { "Content-Type": "application/json" } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  acceptRefreshedToken(response);
  const result = await response.json();
  if (!response.ok) throw new ReviewApiError(result.msg || "댓글 요청에 실패했습니다.", response.status);
  return result.data;
}
