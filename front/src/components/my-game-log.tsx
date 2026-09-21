"use client";

import { useEffect, useState, type FormEvent, type MouseEvent as ReactMouseEvent } from "react";
import { deleteReview, getMyDetailedReview, ReviewApiError, saveDetailedReview } from "@/features/reviews/api";
import type {
  DetailedReview,
  DetailedReviewSaveBody,
  PlayStatus,
} from "@/features/reviews/types";
import type { Option } from "@/lib/games";
import styles from "./my-game-log.module.css";

type Props = {
  gameId: number;
  platforms: Option[];
  accessToken?: string;
  refreshKey?: number;
  onLoginRequired?: () => void;
  onSaved?: () => void;
};

type FormState = {
  playStatus: PlayStatus | "";
  isPlaying: boolean;
  isBacklog: boolean;
  isWishlist: boolean;
  isLiked: boolean;
  platformId: string;
  playTimeHours: string;
  finishTimeHours: string;
  masterTimeHours: string;
  startedAt: string;
  completedAt: string;
  lastPlayedAt: string;
  rating: string;
  content: string;
  spoiler: boolean;
};

function textNumber(value: number | null | undefined) {
  return value == null ? "" : String(value);
}

function initialForm(detail: DetailedReview | null): FormState {
  const record = detail?.userGame;
  const review = detail?.review;
  return {
    // 기존 PLAYED 기록도 화면에서는 플레이 완료로 취급하고,
    // 다음 저장부터 COMPLETED 값으로 통일합니다.
    playStatus: record?.playStatus === "PLAYED" ? "COMPLETED" : record?.playStatus ?? "",
    isPlaying: record?.playing ?? false,
    isBacklog: record?.backlog ?? false,
    isWishlist: record?.wishlist ?? false,
    isLiked: record?.liked ?? false,
    platformId: textNumber(record?.platformId),
    playTimeHours: textNumber(record?.playTimeHours),
    finishTimeHours: textNumber(record?.finishTimeHours),
    masterTimeHours: textNumber(record?.masterTimeHours),
    startedAt: record?.startedAt ?? "",
    completedAt: record?.completedAt ?? "",
    lastPlayedAt: record?.lastPlayedAt?.slice(0, 16) ?? "",
    rating: textNumber(review?.rating),
    content: review?.content ?? "",
    spoiler: review?.spoiler ?? false,
  };
}

function optionalNumber(value: string) {
  return value === "" ? null : Number(value);
}

function toRequest(form: FormState): DetailedReviewSaveBody {
  const hasReview = form.rating !== "" || form.content.trim() !== "";
  return {
    userGame: {
      playStatus: form.playStatus || null,
      isPlaying: form.isPlaying,
      isBacklog: form.isBacklog,
      isWishlist: form.isWishlist,
      isLiked: form.isLiked,
      platformId: optionalNumber(form.platformId),
      playTimeHours: optionalNumber(form.playTimeHours),
      finishTimeHours: optionalNumber(form.finishTimeHours),
      masterTimeHours: optionalNumber(form.masterTimeHours),
      startedAt: form.startedAt || null,
      completedAt: form.completedAt || null,
      lastPlayedAt: form.lastPlayedAt ? `${form.lastPlayedAt}:00` : null,
    },
    review: hasReview ? {
      rating: optionalNumber(form.rating),
      content: form.content.trim(),
      spoiler: form.spoiler,
    } : null,
  };
}

export default function MyGameLog({
  gameId,
  platforms,
  accessToken,
  refreshKey = 0,
  onLoginRequired,
  onSaved,
}: Props) {
  const [tokenVerified, setTokenVerified] = useState(false);
  const [detail, setDetail] = useState<DetailedReview | null>(null);
  const [form, setForm] = useState<FormState>(() => initialForm(null));
  const [editing, setEditing] = useState(false);
  const [showDates, setShowDates] = useState(false);
  const [quickRatingHover, setQuickRatingHover] = useState<number | null>(null);
  const [hoverRating, setHoverRating] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [quickSaving, setQuickSaving] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!accessToken) return;

    const token = accessToken;
    let active = true;

    async function load() {
      setLoading(true);
      try {
        const result = await getMyDetailedReview(gameId, token);
        if (!active) return;
        setDetail(result);
        setForm(initialForm(result));
        setTokenVerified(true);
        setMessage("");
      } catch (reason) {
        if (!active) return;
        setTokenVerified(false);
        setMessage(reason instanceof ReviewApiError && reason.status === 401
          ? "로그인 정보가 만료되었습니다. 다시 로그인해 주세요."
          : reason instanceof Error ? reason.message : "내 기록을 불러오지 못했습니다.");
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();

    return () => { active = false; };
  }, [accessToken, gameId, refreshKey]);

  function openEditor() {
    if (!accessToken || !tokenVerified) {
      setMessage("게임 기록을 작성하려면 로그인이 필요합니다.");
      onLoginRequired?.();
      return;
    }
    setForm(initialForm(detail));
    setShowDates(Boolean(detail?.userGame?.startedAt || detail?.userGame?.completedAt || detail?.userGame?.lastPlayedAt));
    setEditing(true);
    setMessage("");
  }

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function ratingFromPointer(event: ReactMouseEvent<HTMLButtonElement>, score: number) {
    const bounds = event.currentTarget.getBoundingClientRect();
    return event.clientX - bounds.left < bounds.width / 2 ? score - 0.5 : score;
  }

  async function saveQuickChange(nextForm: FormState, successMessage: string) {
    if (!accessToken || !tokenVerified) {
      setMessage("게임 기록을 작성하려면 로그인이 필요합니다.");
      onLoginRequired?.();
      return;
    }

    setForm(nextForm);
    setQuickSaving(true);
    setMessage("");
    try {
      const saved = await saveDetailedReview(gameId, toRequest(nextForm), accessToken);
      setDetail(saved);
      setForm(initialForm(saved));
      setMessage(successMessage);
      onSaved?.();
    } catch (reason) {
      if (reason instanceof ReviewApiError && reason.status === 401) {
        setTokenVerified(false);
      }
      setForm(initialForm(detail));
      setMessage(reason instanceof Error ? reason.message : "게임 기록을 저장하지 못했습니다.");
    } finally {
      setQuickSaving(false);
    }
  }

  function quickUpdate<K extends keyof FormState>(key: K, value: FormState[K], successMessage: string) {
    const nextForm = { ...form, [key]: value };
    void saveQuickChange(nextForm, successMessage);
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!accessToken || !tokenVerified) return;
    const hasReview = form.rating !== "" || form.content.trim() !== "";
    setSaving(true);
    setMessage("");
    try {
      const saved = await saveDetailedReview(gameId, toRequest(form), accessToken);
      setDetail(saved);
      setForm(initialForm(saved));
      setEditing(false);
      setMessage(hasReview ? "게임 기록과 리뷰를 저장했습니다." : "리뷰 없이 게임 기록만 저장했습니다.");
      onSaved?.();
    } catch (reason) {
      if (reason instanceof ReviewApiError && reason.status === 401) {
        setTokenVerified(false);
      }
      setMessage(reason instanceof Error ? reason.message : "게임 기록을 저장하지 못했습니다.");
    } finally {
      setSaving(false);
    }
  }

  async function removeExistingReview() {
    const reviewId = detail?.review?.reviewId;
    if (!reviewId || !accessToken || !tokenVerified || deleting) return;
    if (!window.confirm("작성한 리뷰를 삭제할까요? 게임 기록은 그대로 유지됩니다.")) return;

    setDeleting(true);
    setMessage("");
    try {
      await deleteReview(reviewId, accessToken);
      const nextDetail = detail ? { ...detail, review: null } : null;
      setDetail(nextDetail);
      setForm(initialForm(nextDetail));
      setEditing(false);
      setMessage("리뷰를 삭제했습니다. 게임 기록은 그대로 유지됩니다.");
      onSaved?.();
    } catch (reason) {
      if (reason instanceof ReviewApiError && reason.status === 401) {
        setTokenVerified(false);
      }
      setMessage(reason instanceof Error ? reason.message : "리뷰를 삭제하지 못했습니다.");
    } finally {
      setDeleting(false);
    }
  }

  const visibleDetail = accessToken && tokenVerified ? detail : null;
  const isAuthenticated = Boolean(accessToken && tokenVerified);
  const rating = visibleDetail?.review?.rating ?? null;
  const quickRatingPreview = quickRatingHover ?? (isAuthenticated && form.rating ? Number(form.rating) : 0);
  const ratingPreview = hoverRating ?? (form.rating ? Number(form.rating) : 0);
  const hasReviewInput = form.rating !== "" || form.content.trim() !== "";
  const hasExistingReview = detail?.review != null;
  const playedSelected = form.playStatus === "PLAYED" || form.playStatus === "COMPLETED";
  const quickPlayedSelected = isAuthenticated && playedSelected;
  const quickIsPlaying = isAuthenticated && form.isPlaying;
  const quickIsBacklog = isAuthenticated && form.isBacklog;
  const quickIsWishlist = isAuthenticated && form.isWishlist;
  const quickIsLiked = isAuthenticated && form.isLiked;

  return <section className={styles.panel} aria-label="내 게임 기록">
    <span className={styles.eyebrow}>MY GAME LOG</span>
    <div className={styles.quickRating} aria-label={rating == null ? "내 별점 없음" : `내 별점 ${rating}점`} onMouseLeave={() => setQuickRatingHover(null)}>
      {[1, 2, 3, 4, 5].map((score) => {
        const ratingClass = quickRatingPreview >= score ? styles.quickStarSelected : quickRatingPreview === score - 0.5 ? styles.quickStarHalf : undefined;
        return <button
          key={score}
          type="button"
          className={ratingClass}
          onMouseMove={(event) => setQuickRatingHover(ratingFromPointer(event, score))}
          onClick={(event) => {
            const nextRating = ratingFromPointer(event, score);
            quickUpdate("rating", String(nextRating), `별점 ${nextRating.toFixed(1)}점을 저장했습니다.`);
          }}
          disabled={loading || quickSaving}
          aria-label={`${score - 0.5}점 또는 ${score}점 바로 저장`}
        >★</button>;
      })}
      {isAuthenticated && form.rating && <button
        type="button"
        className={styles.quickRatingClear}
        onClick={() => quickUpdate("rating", "", "별점을 삭제했습니다.")}
        disabled={loading || quickSaving}
        aria-label="별점 삭제"
      >↺</button>}
    </div>
    <div className={styles.quickStatuses} aria-label="게임 상태 빠른 설정">
      <button type="button" className={quickPlayedSelected ? styles.quickSelected : undefined} disabled={loading || quickSaving} onClick={() => quickUpdate("playStatus", quickPlayedSelected ? "" : "COMPLETED", quickPlayedSelected ? "플레이 완료 상태를 해제했습니다." : "플레이 완료로 저장했습니다.")}><span aria-hidden="true">🎮</span>플레이 완료</button>
      <button type="button" className={quickIsPlaying ? styles.quickSelected : undefined} disabled={loading || quickSaving} onClick={() => quickUpdate("isPlaying", !quickIsPlaying, quickIsPlaying ? "플레이 중 상태를 해제했습니다." : "플레이 중으로 저장했습니다.")}><span aria-hidden="true">▶</span>플레이 중</button>
      <button type="button" className={quickIsBacklog ? styles.quickSelected : undefined} disabled={loading || quickSaving} onClick={() => quickUpdate("isBacklog", !quickIsBacklog, quickIsBacklog ? "플레이 예정에서 해제했습니다." : "플레이 예정으로 저장했습니다.")}><span aria-hidden="true">▦</span>플레이 예정</button>
      <button type="button" className={quickIsWishlist ? styles.quickSelected : undefined} disabled={loading || quickSaving} onClick={() => quickUpdate("isWishlist", !quickIsWishlist, quickIsWishlist ? "위시리스트에서 해제했습니다." : "위시리스트에 저장했습니다.")}><span aria-hidden="true">★</span>위시리스트</button>
    </div>
    <div className={styles.quickLikeRow}>
      <span>좋아하는 게임</span>
      <button
        type="button"
        className={quickIsLiked ? styles.quickLikeSelected : undefined}
        disabled={loading || quickSaving}
        aria-label={quickIsLiked ? "좋아하는 게임에서 해제" : "좋아하는 게임으로 표시"}
        aria-pressed={quickIsLiked}
        onClick={() => quickUpdate(
          "isLiked",
          !quickIsLiked,
          quickIsLiked ? "좋아하는 게임에서 해제했습니다." : "좋아하는 게임으로 저장했습니다.",
        )}
      >♥</button>
    </div>
    {quickSaving && <p className={styles.quickProgress} role="status">변경 내용을 저장하는 중…</p>}
    {loading && accessToken
      ? <p className={styles.summary}>내 기록을 불러오는 중…</p>
      : visibleDetail?.review
        ? <p className={styles.summary}>{`내 별점 ${rating ?? "없음"} · 리뷰 작성됨`}</p>
        : null}
    <button type="button" className={styles.editButton} onClick={openEditor} disabled={loading}>
      {loading && accessToken ? "로그인 확인 중…" : isAuthenticated ? (visibleDetail?.userGame ? "기록 · 리뷰 수정" : "기록 · 리뷰 작성") : "로그인 후 기록하기"}
    </button>
    {message && !editing && accessToken && <p className={styles.message} role="status">{message}</p>}

    {editing && accessToken && tokenVerified && <div className={styles.backdrop} role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) setEditing(false);
    }}>
      <form className={styles.dialog} onSubmit={submit} role="dialog" aria-modal="true" aria-labelledby="game-log-title">
        <div className={styles.dialogHeader}><div><span>MY GAME LOG</span><h2 id="game-log-title">게임 기록과 리뷰</h2></div><button type="button" onClick={() => setEditing(false)} aria-label="닫기">×</button></div>
        {message && <p className={styles.dialogMessage} role="alert">{message}</p>}

        <div className={styles.editorLayout}>
          <aside className={styles.statusColumn} aria-label="게임 상태">
            <span className={styles.columnLabel}>내 게임 상태</span>
            <button type="button" className={playedSelected ? styles.selectedStatus : undefined} onClick={() => update("playStatus", playedSelected ? "" : "COMPLETED")}>✓ 플레이 완료</button>
            <button type="button" className={form.isPlaying ? styles.selectedStatus : undefined} onClick={() => update("isPlaying", !form.isPlaying)}>▶ 플레이 중</button>
            <button type="button" className={form.isBacklog ? styles.selectedStatus : undefined} onClick={() => update("isBacklog", !form.isBacklog)}>▦ 플레이 예정</button>
            <button type="button" className={form.isWishlist ? styles.selectedStatus : undefined} onClick={() => update("isWishlist", !form.isWishlist)}>★ 위시리스트</button>
            <button type="button" className={`${styles.likeStatus} ${form.isLiked ? styles.selectedLike : ""}`} onClick={() => update("isLiked", !form.isLiked)}>♥ 좋아하는 게임</button>
            <label className={styles.otherStatus}>기타 상태<select value={playedSelected ? "" : form.playStatus} onChange={(event) => update("playStatus", event.target.value as PlayStatus | "")}><option value="">선택 안 함</option><option value="RETIRED">중단</option><option value="SHELVED">보류</option><option value="ABANDONED">포기</option></select></label>
          </aside>

          <div className={styles.reviewColumn}>
            <div className={styles.topFields}>
              <div className={styles.ratingField}>
                <span>별점</span>
                <div className={styles.ratingButtons} role="group" aria-label="별점 선택" onMouseLeave={() => setHoverRating(null)}>
                  {[1, 2, 3, 4, 5].map((score) => {
                    const ratingClass = ratingPreview >= score ? styles.selectedRating : ratingPreview === score - 0.5 ? styles.halfRating : undefined;
                    return <button
                      key={score}
                      type="button"
                      className={ratingClass}
                      onMouseMove={(event) => setHoverRating(ratingFromPointer(event, score))}
                      onClick={(event) => update("rating", String(ratingFromPointer(event, score)))}
                      aria-label={`${score - 0.5}점 또는 ${score}점 선택`}
                    >★</button>;
                  })}
                  {form.rating && <button type="button" className={styles.clearRating} onClick={() => update("rating", "")} aria-label="별점 지우기">×</button>}
                  <output className={styles.ratingOutput} aria-live="polite">{ratingPreview ? `${ratingPreview.toFixed(1)}점` : "선택 안 함"}</output>
                </div>
              </div>
              <label className={styles.platformField}>플랫폼<select value={form.platformId} onChange={(event) => update("platformId", event.target.value)} disabled={platforms.length === 0}><option value="">{platforms.length ? "플랫폼 선택" : "등록된 플랫폼 정보 없음"}</option>{platforms.map((platform) => <option key={platform.id} value={platform.id}>{platform.name}</option>)}</select></label>
            </div>

            <label className={styles.dateToggle}><input type="checkbox" checked={showDates} onChange={(event) => setShowDates(event.target.checked)} />플레이 날짜 기록하기</label>
            {showDates && <div className={styles.dateGrid}>
              <label>시작일<input type="date" value={form.startedAt} onChange={(event) => update("startedAt", event.target.value)} /></label>
              <label>완료일<input type="date" value={form.completedAt} onChange={(event) => update("completedAt", event.target.value)} /></label>
              <label>마지막 플레이<input type="datetime-local" value={form.lastPlayedAt} onChange={(event) => update("lastPlayedAt", event.target.value)} /></label>
            </div>}

            <label className={styles.reviewLabel}>리뷰 <small>선택</small><textarea rows={7} value={form.content} onChange={(event) => update("content", event.target.value)} placeholder="이 게임은 어땠나요? 별점이나 글 없이 게임 상태만 저장해도 됩니다." /></label>
            <label className={styles.spoiler}><input type="checkbox" checked={form.spoiler} onChange={(event) => update("spoiler", event.target.checked)} />스포일러가 포함되어 있어요</label>
          </div>
        </div>

        <div className={styles.actions}>
          <span className={styles.saveHint}>{hasReviewInput
            ? "게임 기록과 리뷰가 함께 저장됩니다."
            : hasExistingReview
              ? "기존 리뷰는 유지되고 게임 기록만 저장됩니다. 리뷰를 없애려면 리뷰 삭제를 눌러주세요."
              : "별점과 글이 없으므로 게임 기록만 저장됩니다."}</span>
          {hasExistingReview && <button type="button" className={styles.deleteButton} onClick={removeExistingReview} disabled={saving || deleting}>{deleting ? "삭제 중…" : "리뷰 삭제"}</button>}
          <button type="button" onClick={() => setEditing(false)} disabled={saving || deleting}>취소</button>
          <button type="submit" className={styles.saveButton} disabled={saving || deleting}>{saving ? "저장 중…" : hasReviewInput ? "리뷰와 기록 저장" : "게임 기록만 저장"}</button>
        </div>
      </form>
    </div>}
  </section>;
}
