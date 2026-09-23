"use client";

import Link from "next/link";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { createReviewReport, deleteReview, getLikeStatus, getMyDetailedReview, setReviewLike } from "@/features/reviews/api";
import type { DetailedReview } from "@/features/reviews/types";
import type { Review, ReviewPage } from "@/features/reviews/list-types";
import styles from "./game-reviews.module.css";

type ReviewSort = "newest" | "rating";
type ReviewResponse = { data?: ReviewPage; msg?: string };
const pageSize = 5;
const numberFormat = new Intl.NumberFormat("ko-KR");
function getPlayLabel(review: Review) {
  if (review.playing) return "플레이 중";
  if (review.playStatus === "COMPLETED" || review.playStatus === "PLAYED") return "플레이 완료";
  if (review.playStatus === "RETIRED" || review.playStatus === "DROPPED") return "플레이 중단";
  if (review.playStatus === "SHELVED") return "보류";
  if (review.backlog) return "플레이 예정";
  if (review.wishlist) return "위시리스트";
  return null;
}



function ReviewLikeCount({ reviewId, accessToken }: { reviewId: number; accessToken?: string }) {
  const [likeCount, setLikeCount] = useState<number | null>(null);
  const [liked, setLiked] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    getLikeStatus(reviewId, accessToken)
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

function ReviewCard({
  review,
  accessToken,
  mine = false,
  deleting = false,
  onDelete,
  onReport,
}: {
  review: Review;
  accessToken?: string;
  mine?: boolean;
  deleting?: boolean;
  onDelete?: () => void;
  onReport?: () => void;
}) {
  const [revealed, setRevealed] = useState(false);
  const hidden = review.spoiler && !revealed;
  const nickname = review.nickname?.trim() || `플레이어 ${review.userId}`;
  const playLabel = getPlayLabel(review);
  const avatarStyle = review.profileImageUrl
    ? { backgroundImage: `url(${JSON.stringify(review.profileImageUrl)})` }
    : undefined;

  return <article className={styles.card}>
    <div className={styles.cardHeader}>
      <div className={styles.reviewIdentity}>
        <span className={`${styles.avatar} ${review.profileImageUrl ? styles.avatarImage : ""}`} style={avatarStyle} aria-hidden="true">
          {!review.profileImageUrl && nickname.slice(0, 1).toUpperCase()}
        </span>
        <div className={styles.reviewerDetails}>
          <div className={styles.reviewerName}>
            <Link className={styles.reviewerProfileLink} href={`/profile/${review.userId}`}>{nickname}</Link><span>님이 리뷰를 남겼어요</span>
            {mine && <span className={styles.mineBadge}>내 리뷰</span>}
          </div>
          <div className={styles.reviewerMeta}>
            {review.rating == null
              ? <span className={styles.noRating}>별점 없음</span>
              : <span className={styles.rating}><span aria-hidden="true">★</span><strong>{review.rating.toFixed(1)}</strong><small>/ 5.0</small></span>}
            {playLabel && <span className={styles.playState}><i aria-hidden="true" />{playLabel}</span>}
            {review.platformName && <span className={styles.platformName}>{review.platformName}</span>}
          </div>
        </div>
      </div>
      <time dateTime={review.createdDate}>{review.createdDate.slice(0, 10).replaceAll("-", ".")}</time>
    </div>
    <div className={styles.reviewBody}>
      {hidden && <div className={styles.spoilerCover}>
        <span>스포일러가 포함된 리뷰예요.</span>
        <button onClick={() => setRevealed(true)}>내용 보기</button>
      </div>}
      <p className={hidden ? styles.blurred : undefined} aria-hidden={hidden}>
        {review.content}
      </p>
    </div>
    <div className={styles.cardActions}>
      <ReviewLikeCount reviewId={review.reviewId} accessToken={accessToken} />
      {mine && onDelete && <button type="button" className={styles.deleteButton} onClick={onDelete} disabled={deleting}>
        {deleting ? "삭제 중…" : "리뷰 삭제"}
      </button>}
      {!mine && accessToken && onReport && <button type="button" className={styles.reportButton} onClick={onReport}>신고</button>}
      {review.spoiler && revealed && <button className={styles.hideButton} onClick={() => setRevealed(false)}>다시 가리기</button>}
    </div>
  </article>;
}

// 게임 상세 화면의 리뷰 목록과 좋아요 액션
export default function GameReviews({
  gameId,
  accessToken,
  refreshKey = 0,
  onDeleted,
}: {
  gameId: string;
  accessToken?: string;
  refreshKey?: number;
  onDeleted?: () => void;
}) {
  const sortMenuRef = useRef<HTMLDivElement>(null);
  const [sort, setSort] = useState<ReviewSort>("newest");
  const [sortOpen, setSortOpen] = useState(false);
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<ReviewPage | null>(null);
  const [myDetail, setMyDetail] = useState<DetailedReview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [actionNotice, setActionNotice] = useState("");
  const [deletingReviewId, setDeletingReviewId] = useState<number | null>(null);
  const [reportingReview, setReportingReview] = useState<Review | null>(null);
  const [reportReason, setReportReason] = useState("");
  const [reporting, setReporting] = useState(false);
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
        const [response, detail] = await Promise.all([
          fetch(`/api/games/${gameId}/reviews?${query}`, { signal: controller.signal }),
          accessToken
            ? getMyDetailedReview(Number(gameId), accessToken).catch(() => null)
            : Promise.resolve(null),
        ]);
        const body = (await response.json()) as ReviewResponse;
        if (!response.ok || !body.data) throw new Error(body.msg || "리뷰를 불러오지 못했어요.");
        setResult(body.data);
        setMyDetail(detail);
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
  }, [accessToken, gameId, page, refreshKey, sort, retry]);

  async function removeMyReview() {
    const reviewId = myDetail?.review?.reviewId;
    if (!accessToken || !reviewId || deletingReviewId != null) return;
    if (!window.confirm("작성한 리뷰를 삭제할까요? 게임 기록은 그대로 유지됩니다.")) return;

    setDeletingReviewId(reviewId);
    setActionError("");
    setActionNotice("");
    try {
      await deleteReview(reviewId, accessToken);
      setMyDetail((current) => current ? { ...current, review: null } : null);
      setResult((current) => current ? {
        ...current,
        reviews: current.reviews.filter((review) => review.reviewId !== reviewId),
        totalElements: Math.max(0, current.totalElements - 1),
      } : current);
      setPage(0);
      setActionNotice("리뷰를 삭제했습니다. 게임 기록은 그대로 유지됩니다.");
      onDeleted?.();
    } catch (reason) {
      setActionError(reason instanceof Error ? reason.message : "리뷰를 삭제하지 못했어요.");
    } finally {
      setDeletingReviewId(null);
    }
  }

  async function submitReport(event: FormEvent) {
    event.preventDefault();
    const reason = reportReason.trim();
    if (!accessToken || !reportingReview || !reason || reporting) return;

    setReporting(true);
    setActionError("");
    setActionNotice("");
    try {
      await createReviewReport(reportingReview.reviewId, reason, accessToken);
      setReportingReview(null);
      setReportReason("");
      setActionNotice("리뷰 신고가 접수되었습니다. 관리자가 내용을 확인할 예정입니다.");
    } catch (reasonValue) {
      setActionError(reasonValue instanceof Error ? reasonValue.message : "리뷰 신고를 접수하지 못했어요.");
    } finally {
      setReporting(false);
    }
  }

  const myReview = myDetail?.review;
  const visibleReviews = result
    ? page === 0 && myReview
      ? [myReview, ...result.reviews.filter((review) => review.reviewId !== myReview.reviewId)]
      : result.reviews
    : [];

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

    {actionError && <p className={styles.actionError} role="alert">{actionError}</p>}
    {actionNotice && <p className={styles.actionNotice} role="status">{actionNotice}</p>}

    {loading ? <div className={styles.loading} role="status">리뷰를 불러오는 중...</div>
      : error ? <div className={styles.emptyState} role="alert"><h3>리뷰를 불러오지 못했어요.</h3><p>{error}</p><button onClick={() => setRetry(value => value + 1)}>다시 시도 ↗</button></div>
      : !visibleReviews.length ? <div className={styles.emptyState}><h3>아직 등록된 리뷰가 없어요.</h3><p>이 게임의 첫 번째 이야기를 기다리고 있어요.</p></div>
      : <>
        <div className={styles.list}>{visibleReviews.map(review => <ReviewCard
          key={review.reviewId}
          review={review}
          accessToken={accessToken}
          mine={myReview?.reviewId === review.reviewId}
          deleting={deletingReviewId === review.reviewId}
          onDelete={myReview?.reviewId === review.reviewId ? removeMyReview : undefined}
          onReport={myReview?.reviewId !== review.reviewId ? () => {
            setActionError("");
            setActionNotice("");
            setReportReason("");
            setReportingReview(review);
          } : undefined}
        />)}</div>
        {result && result.totalPages > 1 && <nav className={styles.pagination} aria-label="리뷰 페이지 이동">
          <button disabled={page === 0} onClick={() => setPage(value => value - 1)}>← 이전</button>
          <span>{page + 1} / {result.totalPages}</span>
          <button disabled={!result.hasNext} onClick={() => setPage(value => value + 1)}>다음 →</button>
        </nav>}
      </>}

    {reportingReview && <div className={styles.reportBackdrop} role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget && !reporting) setReportingReview(null);
    }}>
      <form className={styles.reportDialog} onSubmit={submitReport} role="dialog" aria-modal="true" aria-labelledby="report-dialog-title">
        <div className={styles.reportDialogHeader}>
          <div><span>REPORT REVIEW</span><h3 id="report-dialog-title">리뷰 신고</h3></div>
          <button type="button" onClick={() => setReportingReview(null)} disabled={reporting} aria-label="신고 창 닫기">×</button>
        </div>
        <p className={styles.reportGuide}>운영자가 확인할 수 있도록 신고 사유를 구체적으로 작성해 주세요.</p>
        <div className={styles.reportedReview}>
          <strong>{reportingReview.rating == null ? "별점 없음" : `★ ${reportingReview.rating.toFixed(1)}`}</strong>
          <p>{reportingReview.content}</p>
        </div>
        <label className={styles.reportReason}>신고 사유
          <textarea value={reportReason} onChange={(event) => setReportReason(event.target.value)} maxLength={255} rows={5} placeholder="욕설, 광고, 스포일러 미표시 등 신고 사유를 입력하세요." required />
          <small>{reportReason.length} / 255</small>
        </label>
        <div className={styles.reportActions}>
          <button type="button" onClick={() => setReportingReview(null)} disabled={reporting}>취소</button>
          <button type="submit" className={styles.reportSubmit} disabled={reporting || !reportReason.trim()}>{reporting ? "접수 중…" : "신고 접수"}</button>
        </div>
      </form>
    </div>}
  </section>;
}
