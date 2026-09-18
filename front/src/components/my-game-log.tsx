"use client";

import { useEffect, useState, type FormEvent, type MouseEvent as ReactMouseEvent } from "react";
import { getMyDetailedReview, ReviewApiError, saveDetailedReview } from "@/features/reviews/api";
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
  onLoginRequired?: () => void;
  onTokenChange?: (accessToken?: string) => void;
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
    playStatus: record?.playStatus ?? "",
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
  onLoginRequired,
  onTokenChange,
  onSaved,
}: Props) {
  const [tokenInput, setTokenInput] = useState("");
  const [temporaryToken, setTemporaryToken] = useState<string>();
  const [tokenVerified, setTokenVerified] = useState(false);
  const [detail, setDetail] = useState<DetailedReview | null>(null);
  const [form, setForm] = useState<FormState>(() => initialForm(null));
  const [editing, setEditing] = useState(false);
  const [showDates, setShowDates] = useState(false);
  const [hoverRating, setHoverRating] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const effectiveAccessToken = accessToken || temporaryToken;

  useEffect(() => {
    if (!effectiveAccessToken) return;

    const token = effectiveAccessToken;
    let active = true;

    async function load() {
      setLoading(true);
      try {
        const result = await getMyDetailedReview(gameId, token);
        if (!active) return;
        setDetail(result);
        setForm(initialForm(result));
        setTokenVerified(true);
        onTokenChange?.(token);
        setMessage("");
      } catch (reason) {
        if (!active) return;
        setTokenVerified(false);
        if (!accessToken) setTemporaryToken(undefined);
        onTokenChange?.(undefined);
        setMessage(reason instanceof ReviewApiError && reason.status === 401
          ? "토큰 인증에 실패했습니다. 새 accessToken을 입력해 주세요."
          : reason instanceof Error ? reason.message : "내 기록을 불러오지 못했습니다.");
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();

    return () => { active = false; };
  }, [accessToken, effectiveAccessToken, gameId, onTokenChange]);

  function connectTemporaryToken(event: FormEvent) {
    event.preventDefault();
    const token = tokenInput.trim().replace(/^Bearer\s+/i, "").replace(/^["']|["']$/g, "");
    if (!token) {
      setMessage("Postman에서 발급받은 accessToken을 입력해 주세요.");
      return;
    }
    setTokenVerified(false);
    setTemporaryToken(token);
    setTokenInput("");
    setMessage("토큰을 확인하는 중입니다.");
  }

  function disconnectTemporaryToken() {
    setTemporaryToken(undefined);
    setTokenVerified(false);
    setDetail(null);
    setForm(initialForm(null));
    setEditing(false);
    onTokenChange?.(undefined);
    setMessage("임시 토큰 연결을 해제했습니다.");
  }

  function openEditor() {
    if (!effectiveAccessToken || !tokenVerified) {
      setMessage("아래 입력창에 임시 accessToken을 먼저 연결해 주세요.");
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

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!effectiveAccessToken || !tokenVerified) return;
    const token = effectiveAccessToken;
    const hasReview = form.rating !== "" || form.content.trim() !== "";
    setSaving(true);
    setMessage("");
    try {
      const saved = await saveDetailedReview(gameId, toRequest(form), token);
      setDetail(saved);
      setForm(initialForm(saved));
      setEditing(false);
      setMessage(hasReview ? "게임 기록과 리뷰를 저장했습니다." : "리뷰 없이 게임 기록만 저장했습니다.");
      onSaved?.();
    } catch (reason) {
      if (reason instanceof ReviewApiError && reason.status === 401) {
        setTokenVerified(false);
        if (!accessToken) setTemporaryToken(undefined);
      }
      setMessage(reason instanceof Error ? reason.message : "게임 기록을 저장하지 못했습니다.");
    } finally {
      setSaving(false);
    }
  }

  const visibleDetail = effectiveAccessToken && tokenVerified ? detail : null;
  const rating = visibleDetail?.review?.rating ?? null;
  const ratingPreview = hoverRating ?? (form.rating ? Number(form.rating) : 0);
  const hasReviewInput = form.rating !== "" || form.content.trim() !== "";

  return <section className={styles.panel} aria-label="내 게임 기록">
    <span className={styles.eyebrow}>MY GAME LOG</span>
    <div className={styles.stars} aria-label={rating == null ? "내 별점 없음" : `내 별점 ${rating}점`}>
      {[1, 2, 3, 4, 5].map((star) => <span key={star} className={rating != null && star <= Math.ceil(rating) ? styles.activeStar : undefined}>★</span>)}
    </div>
    <p className={styles.summary}>{loading && effectiveAccessToken ? "내 기록을 불러오는 중…" : visibleDetail?.review ? `내 별점 ${rating ?? "없음"} · 리뷰 작성됨` : visibleDetail?.userGame ? "게임 기록 저장됨" : "아직 남긴 기록이 없어요."}</p>
    <button type="button" className={styles.editButton} onClick={openEditor} disabled={loading}>
      {loading ? "토큰 확인 중…" : tokenVerified ? (visibleDetail?.userGame ? "기록 · 리뷰 수정" : "기록 · 리뷰 작성") : "임시 토큰으로 기록하기"}
    </button>
    {accessToken && onTokenChange
      ? <button type="button" className={styles.tokenDisconnect} onClick={disconnectTemporaryToken}>임시 토큰 연결 해제</button>
      : temporaryToken
        ? <button type="button" className={styles.tokenDisconnect} onClick={disconnectTemporaryToken}>임시 토큰 연결 해제</button>
        : <form className={styles.tokenForm} onSubmit={connectTemporaryToken}>
        <label htmlFor={`temporary-token-${gameId}`}>개발 테스트용 accessToken</label>
        <input id={`temporary-token-${gameId}`} type="password" value={tokenInput} onChange={(event) => setTokenInput(event.target.value)} placeholder="Bearer 없이 토큰 붙여넣기" autoComplete="off" />
        <button type="submit">연결</button>
      </form>}
    {message && <p className={styles.message} role="status">{message}</p>}

    {editing && effectiveAccessToken && tokenVerified && <div className={styles.backdrop} role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) setEditing(false);
    }}>
      <form className={styles.dialog} onSubmit={submit} role="dialog" aria-modal="true" aria-labelledby="game-log-title">
        <div className={styles.dialogHeader}><div><span>MY GAME LOG</span><h2 id="game-log-title">게임 기록과 리뷰</h2></div><button type="button" onClick={() => setEditing(false)} aria-label="닫기">×</button></div>

        <div className={styles.editorLayout}>
          <aside className={styles.statusColumn} aria-label="게임 상태">
            <span className={styles.columnLabel}>내 게임 상태</span>
            <button type="button" className={form.playStatus === "COMPLETED" ? styles.selectedStatus : undefined} onClick={() => update("playStatus", form.playStatus === "COMPLETED" ? "" : "COMPLETED")}>✓ 완료</button>
            <button type="button" className={form.isPlaying ? styles.selectedStatus : undefined} onClick={() => update("isPlaying", !form.isPlaying)}>▶ 플레이 중</button>
            <button type="button" className={form.isBacklog ? styles.selectedStatus : undefined} onClick={() => update("isBacklog", !form.isBacklog)}>▦ 백로그</button>
            <button type="button" className={form.isWishlist ? styles.selectedStatus : undefined} onClick={() => update("isWishlist", !form.isWishlist)}>★ 위시리스트</button>
            <button type="button" className={`${styles.likeStatus} ${form.isLiked ? styles.selectedLike : ""}`} onClick={() => update("isLiked", !form.isLiked)}>♥ 좋아하는 게임</button>
            <label className={styles.otherStatus}>기타 상태<select value={form.playStatus === "COMPLETED" ? "" : form.playStatus} onChange={(event) => update("playStatus", event.target.value as PlayStatus | "")}><option value="">선택 안 함</option><option value="PLAYED">플레이함</option><option value="RETIRED">중단</option><option value="SHELVED">보류</option><option value="ABANDONED">포기</option></select></label>
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

        <div className={styles.actions}><span className={styles.saveHint}>{hasReviewInput ? "게임 기록과 리뷰가 함께 저장됩니다." : "별점과 글이 없으므로 게임 기록만 저장됩니다."}</span><button type="button" onClick={() => setEditing(false)}>취소</button><button type="submit" className={styles.saveButton} disabled={saving}>{saving ? "저장 중…" : hasReviewInput ? "리뷰와 기록 저장" : "게임 기록만 저장"}</button></div>
      </form>
    </div>}
  </section>;
}
