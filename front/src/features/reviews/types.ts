export type ApiResponse<T> = {
  data: T;
  msg: string;
  resultCode: string;
};

export type Review = {
  reviewId: number;
  userGameId: number;
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

export type PlayStatus = "PLAYED" | "COMPLETED" | "RETIRED" | "SHELVED" | "ABANDONED";

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
