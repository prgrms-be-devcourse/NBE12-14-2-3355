export type FollowKind = "following" | "followers";

export type FollowUserResponseDto = {
  userId: number;
  nickname: string;
  profileImageUrl: string | null;
  followedAt: string | null;
  followedByMe: boolean;
  me: boolean;
};

export type FollowPageResponseDto = {
  users: FollowUserResponseDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

export type FollowStatusResponseDto = {
  targetUserId: number;
  followed: boolean;
};
