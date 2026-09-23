import { acceptRefreshedToken } from "@/features/auth/api";
import type { ApiResponse } from "@/features/auth/types";
import type { FollowKind, FollowPageResponseDto, FollowStatusResponseDto } from "./types";

export class FollowApiError extends Error {
  constructor(message: string, public readonly status: number) {
    super(message);
  }
}

async function request<T>(path: string, token: string | null, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const response = await fetch(`/api/users/${path}`, { ...options, headers, cache: "no-store" });
  acceptRefreshedToken(response);
  const payload = await response.json() as ApiResponse<T>;
  if (!response.ok) throw new FollowApiError(payload.msg || "요청을 처리하지 못했습니다.", response.status);
  return payload.data;
}

export function getFollows(userId: number, kind: FollowKind, page: number, token: string | null, signal?: AbortSignal) {
  return request<FollowPageResponseDto>(`${userId}/${kind}?page=${page}&size=10`, token, { signal });
}

export function setFollowing(userId: number, followed: boolean, token: string) {
  return request<FollowStatusResponseDto>(`me/following/${userId}`, token, { method: followed ? "PUT" : "DELETE" });
}

export function searchUsers(keyword: string, page: number, token: string | null, signal?: AbortSignal, size = 10) {
  const query = new URLSearchParams({ keyword, page: String(page), size: String(size) });
  return request<FollowPageResponseDto>(`search?${query}`, token, { signal });
}
