export type ApiResponse<T> = {
  data: T;
  msg: string;
  resultCode: string;
};

export type UserDto = {
  id: number;
  nickname: string;
  email: string;
  role: "USER" | "ADMIN";
  // 백엔드가 아직 내려주지 않는 필드. 값이 생기기 전까지는 항상 undefined이므로
  // 비교는 반드시 === false / === true로 해서(!user.onboardingCompleted 금지)
  // 필드가 추가되는 순간 자동으로 동작하게 만든다. features/auth/onboarding-status.ts 참고.
  onboardingCompleted?: boolean;
  bio?: string;
  profileImageUrl?: string;
};

export type UpdateProfileRequestBody = {
  nickname: string;
  bio: string | null;
  profileImageUrl: string | null;
};

export type SignupRequestBody = {
  nickname: string;
  email: string;
  password: string;
};

export type LoginRequestBody = {
  email: string;
  password: string;
};

export type LoginResponseDto = {
  user: UserDto;
  accessToken: string;
};

export type TokenResponseDto = {
  accessToken: string;
};

export type PreferredGenre = {
  genreId: number;
  genreName: string;
};

export type PreferredGame = {
  gameId: number;
  gameTitle: string;
};
