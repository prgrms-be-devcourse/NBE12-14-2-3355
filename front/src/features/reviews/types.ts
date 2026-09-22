export type ApiResponse<T> = {
  data: T;
  msg: string;
  resultCode: string;
};

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

export type PlayStatus = "PLAYED" | "COMPLETED" | "RETIRED" | "SHELVED" | "DROPPED";

export type UserGame = {
  id: number;
  userId: number;
  gameId: number;
  platformId: number | null;
  playStatus: PlayStatus | null;
  playing: boolean;
  backlog: boolean;
  wishlist: boolean;
  liked: boolean;
  playTimeHours: number | null;
  finishTimeHours: number | null;
  masterTimeHours: number | null;
  startedAt: string | null;
  completedAt: string | null;
  lastPlayedAt: string | null;
  inLibrary: boolean;
};

export type DetailedReview = {
  userGame: UserGame | null;
  review: Review | null;
};

export type ReviewDraft = {
  rating: number | null;
  content: string;
  spoiler: boolean;
};

export type LikeStatus = {
  reviewId: number;
  likeCount: number;
  liked: boolean;
};

export type ReportStatus = "PENDING" | "APPROVED" | "REJECTED";
export type ReviewStatus = "ACTIVE" | "DELETED_BY_USER" | "HIDDEN_BY_ADMIN";

export type ReviewReport = {
  reportId: number;
  reviewId: number;
  reporterId: number;
  reporterNickname: string;
  reviewWriterId: number;
  reviewWriterNickname: string;
  reason: string;
  reviewRating: number | null;
  reviewContent: string | null;
  reviewSpoiler: boolean;
  reviewStatus: ReviewStatus;
  status: ReportStatus;
  createdDate: string;
  lastModifiedDate: string;
};

export type ReviewReportPage = {
  reports: ReviewReport[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

export type DetailedReviewSaveBody = {
  userGame: {
    playStatus: PlayStatus | null;
    isPlaying: boolean;
    isBacklog: boolean;
    isWishlist: boolean;
    isLiked: boolean;
    platformId: number | null;
    playTimeHours: number | null;
    finishTimeHours: number | null;
    masterTimeHours: number | null;
    startedAt: string | null;
    completedAt: string | null;
    lastPlayedAt: string | null;
  };
  review: ReviewDraft | null;
};
