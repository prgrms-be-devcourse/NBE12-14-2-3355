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
