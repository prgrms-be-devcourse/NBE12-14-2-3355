import type {
  ApiResponse,
  DetailedReview,
  DetailedReviewSaveBody,
  LikeStatus,
  ReportStatus,
  ReviewReport,
  ReviewReportPage,
  ReviewPage,
} from "./types";

type RequestOptions = Omit<RequestInit, "headers"> & {
  accessToken?: string;
};

export class ReviewApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly resultCode?: string,
  ) {
    super(message);
  }
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { accessToken, ...requestInit } = options;
  const headers = new Headers();
  if (options.body) headers.set("Content-Type", "application/json");
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);

  const response = await fetch(`/api/review-api/${path}`, {
    ...requestInit,
    headers,
    cache: "no-store",
  });
  const payload = (await response.json()) as ApiResponse<T>;

  if (!response.ok) {
    throw new ReviewApiError(
      payload.msg || "리뷰 요청을 처리하지 못했습니다.",
      response.status,
      payload.resultCode,
    );
  }

  return payload.data;
}

export function getGameReviews(gameId: number, page: number, size = 5) {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: "createdDate,desc",
  });
  return request<ReviewPage>(`games/${gameId}/reviews?${query}`);
}

export function getMyDetailedReview(gameId: number, accessToken: string) {
  return request<DetailedReview>(`games/${gameId}/reviews/me`, { accessToken });
}

export function saveDetailedReview(
  gameId: number,
  body: DetailedReviewSaveBody,
  accessToken: string,
) {
  return request<DetailedReview>(`games/${gameId}/reviews`, {
    method: "PUT",
    body: JSON.stringify(body),
    accessToken,
  });
}

export function deleteReview(reviewId: number, accessToken: string) {
  return request<null>(`reviews/${reviewId}`, {
    method: "DELETE",
    accessToken,
  });
}

export function getLikeStatus(reviewId: number, accessToken?: string) {
  return request<LikeStatus>(`reviews/${reviewId}/likes/count`, { accessToken });
}

export function setReviewLike(reviewId: number, liked: boolean, accessToken: string) {
  return request<LikeStatus>(`reviews/${reviewId}/likes`, {
    method: liked ? "DELETE" : "POST",
    accessToken,
  });
}

export function createReviewReport(reviewId: number, reason: string, accessToken: string) {
  return request<ReviewReport>(`reviews/${reviewId}/reports`, {
    method: "POST",
    body: JSON.stringify({ reason }),
    accessToken,
  });
}

export function getReviewReports(
  accessToken: string,
  page: number,
  size = 20,
  status?: ReportStatus,
) {
  const query = new URLSearchParams({ page: String(page), size: String(size), sort: "createdDate,desc" });
  if (status) query.set("status", status);
  return request<ReviewReportPage>(`admin/review-reports?${query}`, { accessToken });
}

export function updateReviewReportStatus(
  reportId: number,
  status: Exclude<ReportStatus, "PENDING">,
  accessToken: string,
) {
  return request<ReviewReport>(`admin/review-reports/${reportId}/status`, {
    method: "PUT",
    body: JSON.stringify({ status }),
    accessToken,
  });
}
