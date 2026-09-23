"use client";

import Link from "next/link";
import Image from "next/image";
import { useEffect, useMemo, useState } from "react";
import { getUserReviews, ReviewApiError } from "@/features/reviews/api";
import type { PlayStatus, Review, ReviewPage } from "@/features/reviews/types";
import { coverUrl } from "@/features/games/model";
import styles from "./profile.module.css";

const PAGE_SIZE = 5;

const PLAY_STATUS: Record<PlayStatus, { label: string; className: string }> = {
  PLAYED: { label: "플레이함", className: styles.reviewStatusPlayed },
  COMPLETED: { label: "플레이 완료", className: styles.reviewStatusCompleted },
  RETIRED: { label: "끝냄", className: styles.reviewStatusRetired },
  SHELVED: { label: "잠시 보류", className: styles.reviewStatusShelved },
  DROPPED: { label: "플레이 포기", className: styles.reviewStatusDropped },
};

type SortOrder = "lastModifiedDate,desc" | "lastModifiedDate,asc";

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function releaseYear(value: string | null) {
  if (!value) return null;
  const year = Number(value.slice(0, 4));
  return Number.isFinite(year) ? year : null;
}

function pageNumbers(current: number, total: number) {
  if (total <= 7) return Array.from({ length: total }, (_, index) => index);
  const start = Math.max(0, Math.min(current - 3, total - 7));
  return Array.from({ length: 7 }, (_, index) => start + index);
}

function ReviewCover({ review }: { review: Review }) {
  const [failed, setFailed] = useState(false);
  const src = coverUrl(review.gameCoverImageUrl);

  if (!src || failed) {
    return <div className={styles.profileReviewCoverFallback}>NO COVER</div>;
  }

  return (
    <Image
      className={styles.profileReviewCover}
      src={src}
      alt={`${review.gameTitle} 커버`}
      width={132}
      height={184}
      unoptimized
      onError={() => setFailed(true)}
    />
  );
}

function Rating({ value }: { value: number | null }) {
  if (value == null) return <span className={styles.profileReviewNoRating}>별점 없음</span>;

  return (
    <span className={styles.profileReviewRating} aria-label={`별점 ${value.toFixed(1)}점`}>
      <span aria-hidden="true">★</span> {value.toFixed(1)}
    </span>
  );
}

function ReviewStatus({ review }: { review: Review }) {
  if (review.playStatus) {
    const status = PLAY_STATUS[review.playStatus as PlayStatus];
    if (status) {
      return <span className={`${styles.profileReviewStatus} ${status.className}`}>{status.label}</span>;
    }
  }
  if (review.playing) return <span className={`${styles.profileReviewStatus} ${styles.reviewStatusPlaying}`}>플레이 중</span>;
  if (review.backlog) return <span className={`${styles.profileReviewStatus} ${styles.reviewStatusBacklog}`}>플레이 예정</span>;
  if (review.wishlist) return <span className={`${styles.profileReviewStatus} ${styles.reviewStatusWishlist}`}>위시리스트</span>;
  return null;
}

// 프로필 Reviews 탭의 리뷰 목록
export default function ProfileReviews({
  userId,
  nickname,
  accessToken,
}: {
  userId: number;
  nickname: string;
  accessToken?: string;
}) {
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState<SortOrder>("lastModifiedDate,desc");
  const [result, setResult] = useState<ReviewPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    async function load() {
      setLoading(true);
      setError("");
      try {
        const response = await getUserReviews(userId, page, PAGE_SIZE, sort, accessToken);
        if (active) setResult(response);
      } catch (cause) {
        if (!active) return;
        setError(
          cause instanceof ReviewApiError || cause instanceof Error
            ? cause.message
            : "리뷰 목록을 불러오지 못했습니다.",
        );
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();
    return () => {
      active = false;
    };
  }, [accessToken, page, sort, userId]);

  const pages = useMemo(
    () => pageNumbers(result?.page ?? page, result?.totalPages ?? 0),
    [page, result?.page, result?.totalPages],
  );

  return (
    <section className={styles.profileReviewsSection} aria-labelledby="profile-reviews-title">
      <div className={styles.profileReviewsToolbar}>
        <div>
          <span className={styles.sectionEyebrow}>MY REVIEWS</span>
          <h2 id="profile-reviews-title">
            <strong>{result?.totalLikes ?? 0}</strong>개의 좋아요 · <strong>{result?.totalElements ?? 0}</strong>개의 리뷰
          </h2>
        </div>
        <label className={styles.profileReviewsSort}>
          <span>정렬</span>
          <select
            value={sort}
            onChange={(event) => {
              setSort(event.target.value as SortOrder);
              setPage(0);
            }}
          >
            <option value="lastModifiedDate,desc">최신순</option>
            <option value="lastModifiedDate,asc">오래된순</option>
          </select>
        </label>
      </div>

      {loading ? <p className={styles.loading}>작성한 리뷰를 불러오는 중…</p> : null}
      {!loading && error ? <div className={styles.errorBox}>{error}</div> : null}
      {!loading && !error && result?.reviews.length === 0 ? (
        <div className={styles.emptyBox}>아직 작성한 리뷰가 없습니다.</div>
      ) : null}

      {!loading && !error && result ? (
        <div className={styles.profileReviewsList}>
          {result.reviews.map((review) => (
            <article className={styles.profileReviewItem} key={review.reviewId}>
              <header className={styles.profileReviewTitleRow}>
                <h3>
                  <Link href={`/games/${review.gameId}`}>{review.gameTitle}</Link>
                  {releaseYear(review.gameReleaseDate) ? <small>{releaseYear(review.gameReleaseDate)}</small> : null}
                </h3>
                <time dateTime={review.lastModifiedDate}>{formatDate(review.lastModifiedDate)}</time>
              </header>

              <div className={styles.profileReviewGrid}>
                <Link className={styles.profileReviewCoverLink} href={`/games/${review.gameId}`} tabIndex={-1}>
                  <ReviewCover review={review} />
                </Link>
                <div className={styles.profileReviewBody}>
                  <div className={styles.profileReviewMeta}>
                    <strong>{review.nickname || nickname}</strong>
                    <Rating value={review.rating} />
                    <ReviewStatus review={review} />
                    {review.platformName ? <span className={styles.profileReviewPlatform}>{review.platformName}</span> : null}
                  </div>
                  {review.spoiler ? <span className={styles.profileReviewSpoiler}>스포일러 포함</span> : null}
                  <p>{review.content || "작성한 리뷰 내용이 없습니다."}</p>
                  <footer className={styles.profileReviewFooter}>
                    <span>♥ {review.likeCount.toLocaleString()} 좋아요</span>
                    <Link href={`/games/${review.gameId}`}>리뷰 열기 →</Link>
                  </footer>
                </div>
              </div>
            </article>
          ))}
        </div>
      ) : null}

      {!loading && !error && result && result.totalPages > 1 ? (
        <nav className={styles.profileReviewsPagination} aria-label="리뷰 페이지 이동">
          <button type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>‹ 이전</button>
          <div>
            {pages.map((number) => (
              <button
                type="button"
                key={number}
                aria-current={number === page ? "page" : undefined}
                onClick={() => setPage(number)}
              >
                {number + 1}
              </button>
            ))}
          </div>
          <button type="button" disabled={!result.hasNext} onClick={() => setPage((value) => value + 1)}>다음 ›</button>
        </nav>
      ) : null}
    </section>
  );
}
