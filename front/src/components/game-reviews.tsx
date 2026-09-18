"use client";

import { useEffect, useRef, useState } from "react";
import { getLikeStatus, setReviewLike } from "@/features/reviews/api";
import type { Review, ReviewPage } from "@/lib/reviews";
import styles from "./game-reviews.module.css";

type ReviewSort = "newest" | "rating";
type ReviewResponse = { data?: ReviewPage; msg?: string };
const pageSize = 5;
const numberFormat = new Intl.NumberFormat("ko-KR");

function ReviewLikeCount({ reviewId, accessToken }: { reviewId: number; accessToken?: string }) {
  const [likeCount, setLikeCount] = useState<number | null>(null);
  const [liked, setLiked] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    getLikeStatus(reviewId)
      .then((status) => {
        if (active) {
          setLikeCount(status.likeCount);
          setLiked(status.liked);
          setError("");
        }
      })
      .catch((reason: unknown) => {
        if (active) {
          setLikeCount(null);
          setLiked(false);
          setError(reason instanceof Error ? reason.message : "좋아요 정보를 불러오지 못했어요.");
        }
      });

    return () => { active = false; };
  }, [accessToken, reviewId]);

  async function toggleLike() {
    if (!accessToken || likeCount == null || pending) return;
    setPending(true);
    setError("");
    try {
      const status = await setReviewLike(reviewId, liked, accessToken);
      setLikeCount(status.likeCount);
      setLiked(status.liked);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "좋아요를 처리하지 못했어요.");
    } finally {
      setPending(false);
    }
  }

  return <div className={styles.likeArea}>
    <button
      type="button"
      className={`${styles.likeCount} ${liked ? styles.liked : ""}`}
      onClick={toggleLike}
      disabled={!accessToken || likeCount == null || pending}
      title={accessToken ? (liked ? "좋아요 취소" : "리뷰 좋아요") : "로그인 후 좋아요를 누를 수 있습니다."}
      aria-label={`좋아요 ${likeCount ?? 0}개${liked ? ", 내가 좋아요를 누름" : ""}`}
      aria-pressed={liked}
    >
      <span aria-hidden="true">{liked ? "♥" : "♡"}</span>{pending ? "…" : likeCount == null ? "–" : numberFormat.format(likeCount)}
    </button>
    {error && <span className={styles.likeError} role="status">{error}</span>}
  </div>;
}

function ReviewCard({ review, accessToken }: { review: Review; accessToken?: string }) {
  const [revealed, setRevealed] = useState(false);
  const hidden = review.spoiler && !revealed;

  return <article className={styles.card}>
    <div className={styles.cardHeader}>
      {review.rating == null
        ? <div className={styles.noRating}>별점 없음</div>
        : <div className={styles.rating}><span aria-hidden="true">★</span><strong>{review.rating.toFixed(1)}</strong><small>/ 5.0</small></div>}
      <time dateTime={review.createdDate}>{review.createdDate.slice(0, 10).replaceAll("-", ".")}</time>
    </div>
    <div className={styles.reviewBody}>
      {hidden && <div className={styles.spoilerCover}>
        <span>스포일러가 포함된 리뷰예요.</span>
        <button onClick={() => setRevealed(true)}>내용 보기</button>
      </div>}
      <p className={hidden ? styles.blurred : undefined} aria-hidden={hidden}>
        {review.content?.trim() || "별점만 남긴 리뷰입니다."}
      </p>
    </div>
    <div className={styles.cardActions}>
      <ReviewLikeCount reviewId={review.reviewId} accessToken={accessToken} />
      {review.spoiler && revealed && <button className={styles.hideButton} onClick={() => setRevealed(false)}>다시 가리기</button>}
    </div>
  </article>;
}

export default function GameReviews({
  gameId,
  accessToken,
  refreshKey = 0,
}: {
  gameId: string;
  accessToken?: string;
  refreshKey?: number;
}) {
  const sortMenuRef = useRef<HTMLDivElement>(null);
  const [sort, setSort] = useState<ReviewSort>("newest");
  const [sortOpen, setSortOpen] = useState(false);
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<ReviewPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    if (!sortOpen) return;
    function closeOnOutside(event: PointerEvent) {
      if (event.target instanceof Node && !sortMenuRef.current?.contains(event.target)) setSortOpen(false);
    }
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") setSortOpen(false);
    }
    document.addEventListener("pointerdown", closeOnOutside);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("pointerdown", closeOnOutside);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [sortOpen]);

  useEffect(() => {
    const controller = new AbortController();

    async function loadReviews() {
      setLoading(true);
      setError("");
      try {
        const query = new URLSearchParams({
          page: String(page),
          size: String(pageSize),
          sort: sort === "rating" ? "rating,desc" : "createdDate,desc",
        });
        const response = await fetch(`/api/games/${gameId}/reviews?${query}`, { signal: controller.signal });
        const body = (await response.json()) as ReviewResponse;
        if (!response.ok || !body.data) throw new Error(body.msg || "리뷰를 불러오지 못했어요.");
        setResult(body.data);
      } catch (reason) {
        if (!controller.signal.aborted) {
          setResult(null);
          setError(reason instanceof Error ? reason.message : "리뷰를 불러오지 못했어요.");
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadReviews();
    return () => controller.abort();
  }, [gameId, page, refreshKey, sort, retry]);

  return <section className={styles.section} aria-labelledby="reviews-title" aria-busy={loading}>
    <div className={styles.heading}>
      <div><span className={styles.eyebrow}>PLAYER REVIEWS</span><h2 id="reviews-title">플레이어 리뷰 <span>{result ? numberFormat.format(result.totalElements) : ""}</span></h2>
        <p>이 게임을 플레이한 사람들이 남긴 이야기예요.</p></div>
      <div className={styles.sort}><span>정렬</span><div className={styles.sortControl} ref={sortMenuRef}>
        <button type="button" className={styles.sortTrigger} aria-label="리뷰 정렬 방식" aria-expanded={sortOpen} aria-controls="review-sort-options" onClick={() => setSortOpen(value => !value)}>
          {sort === "newest" ? "최신순" : "높은 평점순"}<span className={styles.chevron} aria-hidden="true" />
        </button>
        {sortOpen && <div id="review-sort-options" className={styles.sortOptions}>
          {([ ["newest", "최신순"], ["rating", "높은 평점순"] ] as const).map(([value, label]) => <button type="button" key={value}
            className={value === sort ? styles.selectedOption : undefined} aria-pressed={value === sort}
            onClick={() => { setSort(value); setPage(0); setSortOpen(false); }}>
            <span className={styles.check} aria-hidden="true">{value === sort ? "✓" : ""}</span>{label}
          </button>)}
        </div>}
      </div></div>
    </div>

    {loading ? <div className={styles.loading} role="status">리뷰를 불러오는 중...</div>
      : error ? <div className={styles.emptyState} role="alert"><h3>리뷰를 불러오지 못했어요.</h3><p>{error}</p><button onClick={() => setRetry(value => value + 1)}>다시 시도 ↗</button></div>
      : !result?.reviews.length ? <div className={styles.emptyState}><h3>아직 등록된 리뷰가 없어요.</h3><p>이 게임의 첫 번째 이야기를 기다리고 있어요.</p></div>
      : <>
        <div className={styles.list}>{result.reviews.map(review => <ReviewCard key={review.reviewId} review={review} accessToken={accessToken} />)}</div>
        {result.totalPages > 1 && <nav className={styles.pagination} aria-label="리뷰 페이지 이동">
          <button disabled={page === 0} onClick={() => setPage(value => value - 1)}>← 이전</button>
          <span>{page + 1} / {result.totalPages}</span>
          <button disabled={!result.hasNext} onClick={() => setPage(value => value + 1)}>다음 →</button>
        </nav>}
      </>}
  </section>;
}
