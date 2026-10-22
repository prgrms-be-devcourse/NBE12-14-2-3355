import type { ApiResponse, PlayStatus } from "@/features/reviews/types";

type BooleanStatus = "playing" | "wishlist" | "backlog" | "liked";

export class LibraryApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly resultCode?: string,
  ) {
    super(message);
  }
}

async function request<T>(path: string, method: "PUT" | "PATCH", accessToken: string) {
  const response = await fetch(`/api/library/games/${path}`, {
    method,
    headers: { Authorization: `Bearer ${accessToken}` },
    cache: "no-store",
  });
  const payload = (await response.json()) as ApiResponse<T>;

  if (!response.ok) {
    throw new LibraryApiError(
      payload.msg || "게임 상태를 변경하지 못했습니다.",
      response.status,
      payload.resultCode,
    );
  }

  return payload.data;
}

export function changePlayStatus(
  gameId: number,
  playStatus: PlayStatus | null,
  accessToken: string,
) {
  const query = new URLSearchParams();
  if (playStatus) query.set("status", playStatus);
  const suffix = query.size ? `?${query}` : "";
  return request<{ playStatus: PlayStatus | null }>(
    `${gameId}/play-status${suffix}`,
    "PUT",
    accessToken,
  );
}

export function changeBooleanStatus(
  gameId: number,
  status: BooleanStatus,
  value: boolean,
  accessToken: string,
) {
  const query = new URLSearchParams({ [status]: String(value) });
  return request<{ status: boolean }>(
    `${gameId}/${status}?${query}`,
    "PATCH",
    accessToken,
  );
}
