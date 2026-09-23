import type { ApiResponse, FavoriteGame, ProfileResponse, UserGameLibraryResponse } from "./types";
import { acceptRefreshedToken } from "@/features/auth/api";

export class ProfileApiError extends Error {
  constructor(message: string, public readonly status: number) {
    super(message);
  }
}

async function request<T>(path: string, options: RequestInit = {}, accessToken?: string): Promise<T> {
  const headers = new Headers(options.headers);
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
  if (options.body) headers.set("Content-Type", "application/json");

  const response = await fetch(`/api/library/games/${path}`, {
    ...options,
    headers,
    cache: "no-store",
  });
  acceptRefreshedToken(response);
  const payload = (await response.json()) as ApiResponse<T>;
  if (!response.ok) throw new ProfileApiError(payload.msg || "프로필 정보를 불러오지 못했습니다.", response.status);
  return payload.data;
}

export function getProfile(
  accessToken?: string,
  userId?: number,
) {
  const query = userId
    ? `/${userId}`
    : "";

  return request<ProfileResponse>(
    `profile${query}`,
    {},
    accessToken,
  );
}

export function updateFavoriteGames(gameIds: number[], accessToken: string) {
  return request<FavoriteGame[]>("favorite-games", {
    method: "PUT",
    body: JSON.stringify({ gameIds }),
  }, accessToken);
}

export function getMyLibraryGames(
  accessToken: string,
  keyword?: string,
  page = 0,
  size = 20,
) {
  const params = new URLSearchParams({
    status: "ALL",
    sort: "RECENT_PLAYED",
    page: String(page),
    size: String(size),
  });

  if (keyword?.trim()) {
    params.set("keyword", keyword.trim());
  }

  return request<UserGameLibraryResponse>(
    `?${params.toString()}`,
    {},
    accessToken,
  );
}

export function getLikedGames(
  accessToken: string | undefined,
  page = 0,
  size = 20,
  sort: "RECENT_PLAYED" | "TITLE" = "RECENT_PLAYED",
  userId?: number,
) {
  const params = new URLSearchParams({
    status: "LIKED",
    sort,
    page: String(page),
    size: String(size),
  });

  return request<UserGameLibraryResponse>(
    `${userId === undefined ? "" : `profile/${userId}/games`}?${params.toString()}`,
    {},
    accessToken,
  );
}
