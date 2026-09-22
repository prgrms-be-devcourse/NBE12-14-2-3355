// 게임별 리뷰 목록 API 응답 모델
export type Review = {
  reviewId: number;
  userGameId: number;
  userId: number;
  nickname: string | null;
  profileImageUrl: string | null;
  playStatus: string | null;
  playing: boolean;
  backlog: boolean;
  wishlist: boolean;
  platformName: string | null;
  rating: number | null;
  content: string | null;
  spoiler: boolean;
  createdDate: string;
  lastModifiedDate: string;
};

export type ReviewPage = {
  reviews: Review[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};
