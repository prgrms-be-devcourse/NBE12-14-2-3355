"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  deleteReview,
  getGameReviews,
  getLikeStatus,
  getMyDetailedReview,
  saveDetailedReview,
  setReviewLike,
} from "@/features/reviews/api";
import type {
  DetailedReview,
  DetailedReviewSaveBody,
  LikeStatus,
  Review,
  ReviewDraft,
  ReviewPage,
} from "@/features/reviews/types";
import styles from "./game-review-section.module.css";

type GameReviewSectionProps = {
  gameId: number;
  accessToken?: string;
  pageSize?: number;
  onLoginRequired?: () => void;
};

const emptyPage: ReviewPage = {
  reviews: [],
  page: 0,
  size: 5,
  totalElements: 0,
  totalPages: 0,
  hasNext: false,
};

function reviewBody(detail: DetailedReview | null, review: ReviewDraft): DetailedReviewSaveBody {
  const record = detail?.userGame;
  return {
    userGame: {
      playStatus: record?.playStatus ?? null,
      isPlaying: record?.playing ?? false,
      isBacklog: record?.backlog ?? false,
      isWishlist: record?.wishlist ?? false,
      isLiked: record?.liked ?? false,
      platformId: record?.platformId ?? null,
      playTimeHours: record?.playTimeHours ?? null,
      finishTimeHours: record?.finishTimeHours ?? null,
      masterTimeHours: record?.masterTimeHours ?? null,
      startedAt: record?.startedAt ?? null,
      completedAt: record?.completedAt ?? null,
      lastPlayedAt: record?.lastPlayedAt ?? null,
    },
    review,
  };
}

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("ko-KR", { year: "numeric", month: "short", day: "numeric" }).format(date);
}

function Rating({ value }: { value: number | null }) {
  if (value == null) return <span className={styles.noRating}>별점 없음</span>;
  return <span className={styles.rating} aria-label={`별점 ${value}점`}>★ {value.toFixed(1)}</span>;
}

function LikeButton({ reviewId, accessToken }: { reviewId: number; accessToken?: string }) {
  const [status, setStatus] = useState<LikeStatus | null>(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    getLikeStatus(reviewId, accessToken)
      .then((result) => {
        if (!active) return;
        setStatus(result);
        setError("");
      })
      .catch((reason: unknown) => {
        if (!active) return;
        setStatus(null);
        setError(reason instanceof Error ? reason.message : "좋아요 정보를 불러오지 못했습니다.");
      });
    return () => { active = false; };
  }, [accessToken, reviewId]);

  async function toggle() {
    if (!accessToken || !status || pending) return;
    setPending(true);
    setError("");
    try {
      setStatus(await setReviewLike(reviewId, status.liked, accessToken));
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "좋아요를 처리하지 못했습니다.");
    } finally {
      setPending(false);
    }
  }

  return (
    <div className={styles.likeArea}>
      <button
        type="button"
        className={`${styles.likeButton} ${status?.liked ? styles.liked : ""}`}
        onClick={toggle}
        disabled={!accessToken || !status || pending}
        title={accessToken ? undefined : "로그인 후 좋아요를 누를 수 있습니다."}
        aria-pressed={status?.liked ?? false}
      >
        {status?.liked ? "♥" : "♡"} {status?.likeCount ?? "–"}
      </button>
      {error && <span className={styles.likeError} role="status">{error}</span>}
    </div>
  );
}

function ReviewCard({
  review,
  mine,
  accessToken,
  onEdit,
  onDelete,
}: {
  review: Review;
  mine: boolean;
  accessToken?: string;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const [revealed, setRevealed] = useState(!review.spoiler);

  return (
    <article className={`${styles.card} ${mine ? styles.myCard : ""}`}>
      <div className={styles.cardHeader}>
        <div>
          <strong>{mine ? "내 리뷰" : `플레이어 리뷰 #${review.reviewId}`}</strong>
          <span>{formatDate(review.lastModifiedDate || review.createdDate)}</span>
        </div>
        <Rating value={review.rating} />
      </div>

      {review.spoiler && !revealed ? (
        <button type="button" className={styles.spoiler} onClick={() => setRevealed(true)}>
          스포일러가 포함된 리뷰입니다 · 눌러서 보기
        </button>
      ) : (
        <p className={styles.content}>{review.content?.trim() || "작성된 리뷰 내용이 없습니다."}</p>
      )}

      <div className={styles.cardActions}>
        <LikeButton reviewId={review.reviewId} accessToken={accessToken} />
        {mine && (
          <>
            <button type="button" onClick={onEdit}>수정</button>
            <button type="button" className={styles.danger} onClick={onDelete}>삭제</button>
          </>
        )}
      </div>
    </article>
  );
}

function ReviewEditor({
  detail,
  saving,
  onCancel,
  onSave,
}: {
  detail: DetailedReview | null;
  saving: boolean;
  onCancel: () => void;
  onSave: (draft: ReviewDraft) => Promise<void>;
}) {
  const existing = detail?.review;
  const [rating, setRating] = useState(existing?.rating?.toString() ?? "");
  const [content, setContent] = useState(existing?.content ?? "");
  const [spoiler, setSpoiler] = useState(existing?.spoiler ?? false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!rating && !content.trim()) {
      setError("별점 또는 리뷰 내용 중 하나는 입력해 주세요.");
      return;
    }
    setError("");
    await onSave({ rating: rating ? Number(rating) : null, content: content.trim(), spoiler });
  }

  return (
    <form className={styles.editor} onSubmit={submit}>
      <div className={styles.editorHeading}>
        <div><span>MY GAME LOG</span><h3>{existing ? "내 리뷰 수정" : "내 리뷰 작성"}</h3></div>
        <button type="button" onClick={onCancel} aria-label="리뷰 작성 닫기">×</button>
      </div>
      <label>
        별점 <small>선택</small>
        <select value={rating} onChange={(event) => setRating(event.target.value)}>
          <option value="">별점 없음</option>
          {Array.from({ length: 10 }, (_, index) => (index + 1) / 2).map((score) => (
            <option value={score} key={score}>{score.toFixed(1)}</option>
          ))}
        </select>
      </label>
      <label>
        리뷰 내용 <small>선택</small>
        <textarea
          value={content}
          onChange={(event) => setContent(event.target.value)}
          placeholder="이 게임에서 기억에 남은 경험을 적어보세요."
          rows={6}
        />
      </label>
      <label className={styles.checkLabel}>
        <input type="checkbox" checked={spoiler} onChange={(event) => setSpoiler(event.target.checked)} />
        스포일러가 포함되어 있어요
      </label>
      {error && <p className={styles.formError} role="alert">{error}</p>}
      <div className={styles.editorActions}>
        <button type="button" onClick={onCancel}>취소</button>
        <button type="submit" className={styles.primaryButton} disabled={saving}>
          {saving ? "저장 중…" : "리뷰 저장"}
        </button>
      </div>
    </form>
  );
}

export default function GameReviewSection({
  gameId,
  accessToken,
  pageSize = 5,
  onLoginRequired,
}: GameReviewSectionProps) {
  const [reviews, setReviews] = useState<ReviewPage>({ ...emptyPage, size: pageSize });
  const [myDetail, setMyDetail] = useState<DetailedReview | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  const refresh = useCallback(() => setRefreshKey((value) => value + 1), []);

  useEffect(() => {
    let active = true;

    async function load() {
      setLoading(true);
      setError("");
      try {
        const [reviewPage, detail] = await Promise.all([
          getGameReviews(gameId, page, pageSize),
          accessToken ? getMyDetailedReview(gameId, accessToken) : Promise.resolve(null),
        ]);
        if (!active) return;
        setReviews(reviewPage);
        setMyDetail(detail);
      } catch (reason: unknown) {
        if (active) setError(reason instanceof Error ? reason.message : "리뷰를 불러오지 못했습니다.");
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();

    return () => { active = false; };
  }, [accessToken, gameId, page, pageSize, refreshKey]);

  async function save(draft: ReviewDraft) {
    if (!accessToken) return;
    setSaving(true);
    setError("");
    try {
      await saveDetailedReview(gameId, reviewBody(myDetail, draft), accessToken);
      setEditing(false);
      setPage(0);
      refresh();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "리뷰를 저장하지 못했습니다.");
    } finally {
      setSaving(false);
    }
  }

  async function remove() {
    if (!accessToken || !myDetail?.review) return;
    if (!window.confirm("작성한 리뷰를 삭제할까요? 게임 기록은 유지됩니다.")) return;
    setError("");
    try {
      await deleteReview(myDetail.review.reviewId, accessToken);
      setEditing(false);
      refresh();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "리뷰를 삭제하지 못했습니다.");
    }
  }

  function openEditor() {
    if (!accessToken) {
      onLoginRequired?.();
      return;
    }
    setEditing(true);
  }

  return (
    <section className={styles.section} aria-labelledby={`game-${gameId}-reviews`}>
      <div className={styles.heading}>
        <div>
          <span className={styles.eyebrow}>PLAYER NOTES</span>
          <h2 id={`game-${gameId}-reviews`}>플레이어 리뷰</h2>
          <p>{reviews.totalElements.toLocaleString()}명이 이 게임의 경험을 남겼어요.</p>
        </div>
        <button
          type="button"
          className={styles.primaryButton}
          onClick={openEditor}
          disabled={!accessToken && !onLoginRequired}
        >
          {myDetail?.review ? "내 리뷰 수정" : accessToken ? "리뷰 작성" : "로그인 후 작성"}
        </button>
      </div>

      {editing && accessToken && (
        <ReviewEditor
          key={myDetail?.review?.reviewId ?? "new"}
          detail={myDetail}
          saving={saving}
          onCancel={() => setEditing(false)}
          onSave={save}
        />
      )}

      {error && <div className={styles.error} role="alert">{error}<button type="button" onClick={refresh}>다시 시도</button></div>}

      {loading ? (
        <div className={styles.loading} aria-live="polite">리뷰를 불러오는 중…</div>
      ) : reviews.reviews.length === 0 ? (
        <div className={styles.empty}>
          <strong>아직 작성된 리뷰가 없어요.</strong>
          <p>첫 번째 플레이 기록을 남겨보세요.</p>
        </div>
      ) : (
        <div className={styles.list}>
          {reviews.reviews.map((review) => (
            <ReviewCard
              key={review.reviewId}
              review={review}
              mine={myDetail?.review?.reviewId === review.reviewId}
              accessToken={accessToken}
              onEdit={openEditor}
              onDelete={remove}
            />
          ))}
        </div>
      )}

      {reviews.totalPages > 1 && (
        <nav className={styles.pagination} aria-label="리뷰 페이지 이동">
          <button type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>이전</button>
          <span>{page + 1} / {reviews.totalPages}</span>
          <button type="button" disabled={!reviews.hasNext} onClick={() => setPage((value) => value + 1)}>다음</button>
        </nav>
      )}
    </section>
  );
}
