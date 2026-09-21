import type {
  ApiResponse,
  LoginRequestBody,
  LoginResponseDto,
  PreferredGame,
  PreferredGenre,
  SignupRequestBody,
  TokenResponseDto,
  UpdateProfileRequestBody,
  UserDto,
} from "./types";

type RequestOptions = Omit<RequestInit, "headers"> & {
  accessToken?: string;
};

export class AuthApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly resultCode?: string,
  ) {
    super(message);
  }
}

// 백엔드 자동 재발급 필터가 응답 헤더로 새 accessToken을 보내주면, auth-context가 여기 등록해서 받아감.
let onTokenRefreshed: ((accessToken: string) => void) | null = null;

export function setTokenRefreshedListener(listener: ((accessToken: string) => void) | null) {
  onTokenRefreshed = listener;
}

// 인증이 필요한 다른 API에서도 재발급된 토큰을 기존 인증 상태에 반영
export function acceptRefreshedToken(response: Response) {
  const token = response.headers.get("New-Access-Token");
  if (token) onTokenRefreshed?.(token);
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { accessToken, ...requestInit } = options;
  const headers = new Headers();
  if (options.body) headers.set("Content-Type", "application/json");
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);

  const response = await fetch(`/api/users/${path}`, {
    ...requestInit,
    headers,
    cache: "no-store",
  });

  acceptRefreshedToken(response);

  const payload = (await response.json()) as ApiResponse<T>;

  if (!response.ok) {
    throw new AuthApiError(
      payload.msg || "요청을 처리하지 못했습니다.",
      response.status,
      payload.resultCode,
    );
  }

  return payload.data;
}

export function signup(body: SignupRequestBody) {
  return request<UserDto>("signup", { method: "POST", body: JSON.stringify(body) });
}

export function login(body: LoginRequestBody) {
  return request<LoginResponseDto>("login", { method: "POST", body: JSON.stringify(body) });
}

export function logout() {
  return request<null>("logout", { method: "POST" });
}

export function refresh() {
  return request<TokenResponseDto>("refresh", { method: "POST" });
}

export function getMe(accessToken: string) {
  return request<UserDto>("me", { accessToken });
}

export function updateProfile(body: UpdateProfileRequestBody, accessToken: string) {
  return request<UserDto>("me", { method: "PATCH", body: JSON.stringify(body), accessToken });
}

export function completeOnboarding(accessToken: string) {
  return request<UserDto>("me/onboarding", { method: "PATCH", accessToken });
}

export function skipOnboarding(accessToken: string) {
  return request<UserDto>("me/onboarding/skip", { method: "PATCH", accessToken });
}

export function getPreferredGenres(accessToken: string) {
  return request<PreferredGenre[]>("me/preferred-genres", { accessToken });
}

export function setPreferredGenres(genreIds: number[], accessToken: string) {
  return request<PreferredGenre[]>("me/preferred-genres", {
    method: "PUT",
    body: JSON.stringify({ genreIds }),
    accessToken,
  });
}

export function getPreferredGames(accessToken: string) {
  return request<PreferredGame[]>("me/preferred-games", { accessToken });
}

export function setPreferredGames(gameIds: number[], accessToken: string) {
  return request<PreferredGame[]>("me/preferred-games", {
    method: "PUT",
    body: JSON.stringify({ gameIds }),
    accessToken,
  });
}

export function checkEmailDuplicate(email: string) {
  const query = new URLSearchParams({ email });
  return request<boolean>(`check-email?${query}`);
}

export function checkNicknameDuplicate(nickname: string) {
  const query = new URLSearchParams({ nickname });
  return request<boolean>(`check-nickname?${query}`);
}
