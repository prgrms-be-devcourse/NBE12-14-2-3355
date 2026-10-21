"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { useAuth } from "@/features/auth/auth-context";
import { ReviewApiError, setReviewLike } from "@/features/reviews/api";
import { getFeed, type CommunityReview, type Feed, type FeedTab } from "./api";
import ImageWithFallback from "@/components/ui/image-with-fallback";
import { coverUrl } from "@/features/games/model";
import styles from "./community.module.css";
import ReviewComments from "@/features/reviews/components/review-comments";
import CommentReturn from "@/features/reviews/components/comment-return";
import { communityReturnPath } from "@/features/auth/login-destination";
import { getPlayLabel } from "@/features/reviews/play-label";

export default function Community() {
  const auth = useAuth();
  const router = useRouter();
  const params = useSearchParams();
  const tab: FeedTab = params.get("tab") === "popular" ? "popular" : "following";
  const returnTo = communityReturnPath(`/community?${params}`) ?? "/community";
  const loginPath = `/login?next=${encodeURIComponent(returnTo)}`;

  useEffect(() => {
    if (auth.status === "unauthenticated") router.replace(loginPath);
  }, [auth.status, router, loginPath]);

  if (auth.status !== "authenticated" || !auth.accessToken) {
    return <p className={styles.state} role="status">로그인 상태를 확인하는 중…</p>;
  }

  return <main className={styles.page}>
    <header className={styles.heading}>
      <span>PLAY. SHARE. CONNECT.</span>
      <h1>피드</h1>
      <p>함께하는 게이머들의 이야기에서 다음 게임을 발견하세요.</p>
    </header>
    <nav className={styles.tabs} aria-label="피드 리뷰">
      <Link href="/community" aria-current={tab === "following" ? "page" : undefined}>팔로잉</Link>
      <Link href="/community?tab=popular" aria-current={tab === "popular" ? "page" : undefined}>인기</Link>
    </nav>
    <CommentReturn />
    <FeedList key={`${auth.user?.id}:${tab}`} tab={tab} loginPath={loginPath} />
  </main>;
}

function FeedList({ tab, loginPath }: { tab: FeedTab; loginPath: string }) {
  const { accessToken, refresh, logout } = useAuth();
  const router = useRouter();
  const tokenRef = useRef(accessToken);
  const lifetime = useRef<AbortController | null>(null);
  const [feed, setFeed] = useState<Feed | null>(null);
  const [page, setPage] = useState(0);
  const [pending, setPending] = useState(true);
  const [error, setError] = useState("");
  const [failedPage, setFailedPage] = useState(0);
  const [attempt, setAttempt] = useState(0);
  const loadingMore = useRef(false);

  useEffect(() => { tokenRef.current = accessToken; }, [accessToken]);

  const authorized = useCallback(async <T,>(operation: (token: string) => Promise<T>): Promise<T> => {
    try {
      return await operation(tokenRef.current!);
    } catch (reason) {
      if (!(reason instanceof ReviewApiError) || reason.status !== 401) throw reason;
      const token = await refresh();
      if (lifetime.current?.signal.aborted) throw reason;
      if (!token) {
        router.replace(loginPath);
        throw reason;
      }
      tokenRef.current = token;
      try {
        return await operation(token);
      } catch (retryError) {
        if (retryError instanceof ReviewApiError && retryError.status === 401) {
          await logout();
          router.replace(loginPath);
        }
        throw retryError;
      }
    }
  }, [refresh, logout, router, loginPath]);

  useEffect(() => {
    const controller = new AbortController();
    lifetime.current = controller;
    authorized(token => getFeed(tab, 0, token, controller.signal))
      .then(data => {
        if (!controller.signal.aborted) { setFeed(data); setPage(0); }
      })
      .catch(reason => {
        if (!controller.signal.aborted) {
          setFailedPage(0);
          setError(reason instanceof Error ? reason.message : "리뷰를 불러오지 못했습니다.");
        }
      })
      .finally(() => { if (!controller.signal.aborted) setPending(false); });
    return () => controller.abort();
  }, [tab, attempt, authorized]);

  function reload() {
    setPending(true);
    setError("");
    setAttempt(value => value + 1);
  }

  async function loadMore() {
    const signal = lifetime.current?.signal;
    if (!signal || loadingMore.current) return;
    loadingMore.current = true;
    setPending(true);
    setError("");
    try {
      const data = await authorized(token => getFeed(tab, page + 1, token, signal));
      if (signal.aborted) return;
      setFeed(current => {
        const existing = current?.reviews ?? [];
        const ids = new Set(existing.map(review => review.reviewId));
        return { ...data, reviews: [...existing, ...data.reviews.filter(review => !ids.has(review.reviewId))] };
      });
      setPage(current => current + 1);
    } catch (reason) {
      if (!signal.aborted) {
        setFailedPage(page + 1);
        setError(reason instanceof Error ? reason.message : "리뷰를 불러오지 못했습니다.");
      }
    } finally {
      loadingMore.current = false;
      if (!signal.aborted) setPending(false);
    }
  }

  return <section aria-label={tab === "following" ? "팔로잉 리뷰" : "최근 7일 인기 리뷰"}>
    <div className={styles.sectionHeading}>
      <div><h2>{tab === "following" ? "팔로잉 리뷰" : "최근 7일 인기 리뷰"}</h2>
        <p>{tab === "following" ? "팔로우한 사람들이 남긴 리뷰를 최신순으로 만나보세요." : "최근 168시간에 작성된 리뷰 중 좋아요가 많은 상위 20개입니다."}</p>
      </div>
      <button disabled={pending} onClick={reload}>새로고침</button>
    </div>
    {feed?.reviews.map(review => <ReviewCard key={review.reviewId} review={review}
      onLike={() => authorized(token => setReviewLike(review.reviewId, review.liked, token)).then(result => {
        if (!lifetime.current?.signal.aborted) setFeed(current => current && ({
          ...current, reviews: current.reviews.map(item => item.reviewId === review.reviewId ? { ...item, ...result } : item),
        }));
      })} />)}
    {!pending && !error && feed?.reviews.length === 0 && <div className={styles.state}>
      <h3>{tab === "popular" ? "최근 7일 인기 리뷰가 없습니다."
        : feed.hasFollowing ? "아직 등록된 리뷰가 없습니다." : "팔로우한 사람이 없습니다."}</h3>
      <p>{tab === "popular" ? "새로운 리뷰에 첫 공감을 남겨보세요." : "인기 리뷰에서 마음에 드는 작성자를 만나보세요."}</p>
      {tab === "following" && <Link href="/community?tab=popular">인기 리뷰 둘러보기 →</Link>}
    </div>}
    {pending && <p className={styles.state} role="status">리뷰를 불러오는 중…</p>}
    {error && <div className={styles.state} role="alert"><p>{error}</p>
      <button disabled={pending} onClick={() => failedPage > 0 ? void loadMore() : reload()}>다시 시도</button>
    </div>}
    {feed?.hasNext && !error && <button className={styles.more} disabled={pending} onClick={() => void loadMore()}>더 보기</button>}
  </section>;
}

function ReviewCard({ review, onLike }: { review: CommunityReview; onLike: () => Promise<void> }) {
  const [revealed, setRevealed] = useState(false);
  const [expanded, setExpanded] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const locked = useRef(false);
  const hidden = review.spoiler && !revealed;
  const nickname = review.nickname?.trim() || `플레이어 ${review.userId}`;
  const playLabel = getPlayLabel(review);

  async function like() {
    if (locked.current) return;
    locked.current = true;
    setPending(true);
    setError("");
    try { await onLike(); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "좋아요를 변경하지 못했습니다."); }
    finally { locked.current = false; setPending(false); }
  }

  return <article className={styles.card}>
    <header className={styles.cardHeader}>
      <div className={styles.identity}>
        <Link className={styles.avatar} href={`/profile/${review.userId}`} aria-label={`${nickname}님의 프로필`}>
          <ImageWithFallback src={review.profileImageUrl ?? ""} alt=""
            fallback={<span>{nickname.slice(0, 1).toUpperCase()}</span>} />
        </Link>
        <div className={styles.reviewerDetails}>
          <div className={styles.reviewerName}>
            <Link className={styles.author} href={`/profile/${review.userId}`}>{nickname}</Link>
            <span>님이 리뷰를 남겼습니다</span>
          </div>
          <div className={styles.reviewerMeta}>
          {review.rating == null ? <span className={styles.noRating}>별점 없음</span>
            : <span className={styles.rating}><span aria-hidden="true">★</span><strong>{review.rating.toFixed(1)}</strong><small>/ 5.0</small></span>}
          {playLabel && <span className={styles.playState}><i aria-hidden="true" />{playLabel}</span>}
          </div>
        </div>
      </div>
      <time dateTime={review.createdDate}>{review.createdDate.slice(0, 10).replaceAll("-", ".")}</time>
    </header>
    <Link className={styles.game} href={`/games/${review.gameId}`}>
      <span className={styles.cover}><ImageWithFallback src={coverUrl(review.gameCoverImageUrl)} alt=""
        fallback={<span className={styles.coverFallback} aria-hidden="true">GL</span>} /></span>
      <strong>{review.gameTitle}</strong>
      <span className={styles.arrow} aria-hidden="true">↗</span>
    </Link>
    {hidden ? <div className={styles.spoiler}><p>스포일러가 포함된 리뷰입니다.</p>
      <button onClick={() => setRevealed(true)}>내용 보기</button>
    </div> : <div className={styles.body}>
      <p>{!expanded && review.content.length > 300 ? `${review.content.slice(0, 300)}…` : review.content}</p>
      {review.content.length > 300 && <button aria-expanded={expanded} onClick={() => setExpanded(value => !value)}>{expanded ? "접기" : "본문 펼치기"}</button>}
    </div>}
    <div className={styles.actions}>
      <button aria-label={`좋아요 ${review.likeCount}개${review.liked ? ", 내가 좋아요를 누름" : ""}`}
        title={review.liked ? "좋아요 취소" : "리뷰 좋아요"}
        aria-pressed={review.liked} disabled={pending} onClick={() => void like()}>
        <span aria-hidden="true">{review.liked ? "♥" : "♡"}</span>{review.likeCount.toLocaleString("ko-KR")}
      </button>
    </div>
    {error && <p className={styles.error} role="alert">{error}</p>}
    <div className={styles.comments}><ReviewComments reviewId={review.reviewId} locked={hidden} /></div>
  </article>;
}
