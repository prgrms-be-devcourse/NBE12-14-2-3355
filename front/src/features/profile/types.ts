export type ApiResponse<T> = { data: T; msg: string; resultCode: string };

export type FavoriteGame = {
  gameId: number;
  title: string;
  coverImageUrl: string | null;
  displayOrder: number;
};

export type ProfileStats = {
  playedGameCount: number;
  averageRating: number | null;
  totalPlayTime: number;
};

export type ScatterGame = {
  gameId: number;
  title: string;
  coverImage: string | null;
  playTime: number;
  rating: number | null;
};

export type TasteMetric = {
  ratio: number;
  message: string;
  description: string;
};

export type TasteResponse = {
  longPlay: TasteMetric;
  rating: TasteMetric;
  completion: TasteMetric;
};

export type GenreDistribution = {
  genreName: string;
  ratio: number;
};

export type RecentGame = {
  gameId: number;
  title: string;
  coverImageUrl: string | null;
  playStatus: string | null;
  playing: boolean;
  backlog: boolean;
  wishlist: boolean;
  liked: boolean;
};

export type RecentReview = {
  reviewId: number;
  gameId: number;
  gameTitle: string;
  gameCoverImageUrl: string | null;
  playStatus: string | null;
  platform: string | null;
  rating: number | null;
  content: string | null;
  lastModifiedDate: string;
};

export type ProfileResponse = {
  favorite: FavoriteGame[];
  stats: ProfileStats;
  scatterData: ScatterGame[];
  tasteResponse: TasteResponse;
  genreDistribution: GenreDistribution[];
  recentGames: RecentGame[];
  recentReviews: RecentReview[];
};

export type UserGame = {
  gameId: number;
  title: string;
  coverImageUrl: string | null;
  playStatus: string | null;
  playing: boolean;
  backlog: boolean;
  wishlist: boolean;
  liked: boolean;
};

export type UserGameLibraryResponse = {
  totalElements: number;
  totalPages: number;
  userGames: UserGame[];
};
