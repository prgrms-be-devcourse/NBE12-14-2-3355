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
