"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { coverUrl, type GameDetail, type GameStatistics } from "@/lib/games";
import GameReviews from "@/components/game-reviews";
import RelatedGames from "@/components/related-games";
import MyGameLog from "@/components/my-game-log";
import AuthNav from "@/components/auth/auth-nav";
import { useAuth } from "@/features/auth/auth-context";
import styles from "./game-detail-view.module.css";

type DetailResponse = { data?: GameDetail; msg?: string };
const numberFormat = new Intl.NumberFormat("ko-KR");
const releaseDateFormat = new Intl.DateTimeFormat("en-US", { month: "short", day: "2-digit", year: "numeric", timeZone: "UTC" });

function formatReleaseDate(value: string) {
  const date = new Date(`${value}T00:00:00Z`);
  return Number.isNaN(date.getTime()) ? value : releaseDateFormat.format(date);
}

function GameCover({ game }: { game: GameDetail }) {
  const [broken, setBroken] = useState(false);
  const url = coverUrl(game.coverImageUrl);

  if (!url || broken) {
    return <div className={styles.coverFallback}><span>{game.title}</span><small>커버 준비 중</small></div>;
  }

  // eslint-disable-next-line @next/next/no-img-element
  return <img src={url} alt={`${game.title} 커버`} onError={() => setBroken(true)} />;
}

type StatIconName = "game" | "play" | "backlog" | "wishlist" | "review" | "heart" | "clock";

function StatIcon({ name }: { name: StatIconName }) {
  const paths = {
    game: <><path d="M7 7h10c3 0 5 10 3 11-2 1-4-3-5-3H9c-1 0-3 4-5 3C2 17 4 7 7 7Z"/><path d="M6 11h5M8.5 8.5v5M16 11h.01M18 13h.01"/></>,
    play: <path d="m8 5 11 7-11 7Z"/>,
    backlog: <><rect x="4" y="4" width="5" height="16" rx="1"/><rect x="9" y="4" width="5" height="16" rx="1"/><path d="m15 5 4-1 4 15-4 1ZM5 8h8M5 16h8"/></>,
    wishlist: <><path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9Z"/></>,
    review: <><path d="M5 4h14v12H9l-4 4ZM8 8h8M8 12h5"/></>,
    heart: <path d="M20 5c-2-2-5-1-8 2-3-3-6-4-8-2-4 4 2 10 8 15 6-5 12-11 8-15Z"/>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 6v6l4 2"/></>,
  };
  return <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>;
}

function GameStats({ statistics }: { statistics: GameStatistics }) {
  const distribution = statistics.ratingDistribution ?? [];
  const maxCount = Math.max(1, ...distribution.map(item => item.count));
  const statuses = [
    { label: "플레이 완료", english: "Played", icon: "game", count: statistics.playedCount },
    { label: "플레이 중", english: "Playing", icon: "play", count: statistics.playingCount },
    { label: "플레이 예정", english: "Backlog", icon: "backlog", count: statistics.backlogCount },
    { label: "위시리스트", english: "Wishlist", icon: "wishlist", count: statistics.wishlistCount },
  ] as const;

  return <section className={styles.statsSection} aria-labelledby="stats-title">
    <div className={styles.sectionHeading}>
      <div><span className={styles.eyebrow}>THE COMMUNITY AT A GLANCE</span><h2 id="stats-title">이 게임의 기록</h2></div>
      <p>GameLog 사용자들의 기록을 모았어요.</p>
    </div>
    <div className={styles.statsGrid}>
      <div className={`${styles.statCard} ${styles.ratingCard}`}>
        <div className={styles.ratingSummary}>
          <span className={styles.cardLabel}>GAMELOG RATING</span>
          <div className={styles.ratingValue}>{statistics.averageRating.toFixed(1)}<small>/ 5.0</small></div>
          <p className={styles.cardHint}>{statistics.reviewCount === 0 ? "아직 평가 없음" : `${numberFormat.format(statistics.reviewCount)}개 리뷰 기준`}</p>
        </div>
        <div className={styles.distribution} role="img" aria-label={distribution.map(item => `${item.rating}점 ${item.count}명`).join(", ") || "평점 데이터 없음"}>
          {distribution.map(item => <div className={styles.distributionItem} key={item.rating} aria-hidden="true">
            <span className={styles.barTrack}><span className={styles.barFill} style={{ height: `${item.count / maxCount * 100}%` }} /></span>
          </div>)}
        </div>
      </div>
      <div className={`${styles.statCard} ${styles.statusCard}`} role="group" aria-label="게임 등록 상태별 사용자 수">
        <div className={styles.statusList}>{statuses.map(status => <div className={styles.statusRow} key={status.english}>
          <span><span className={styles.iconBadge}><StatIcon name={status.icon} /></span>{status.label}<small>{status.english}</small></span><strong>{numberFormat.format(status.count)}</strong>
        </div>)}</div>
      </div>
      <div className={styles.metricTiles}>
        <div className={styles.metricTile}><strong>{numberFormat.format(statistics.reviewCount)}</strong><span><StatIcon name="review" />리뷰</span></div>
        <div className={styles.metricTile}><strong>{numberFormat.format(statistics.likeCount)}</strong><span><StatIcon name="heart" />좋아요</span></div>
        <div className={`${styles.metricTile} ${styles.timeTile}`}><strong>{statistics.playTimeUserCount === 0 ? "—" : `${statistics.averagePlayTimeHours.toFixed(1)}h`}</strong><span><StatIcon name="clock" />평균 플레이 시간</span>{statistics.playTimeUserCount > 0 && <small>{numberFormat.format(statistics.playTimeUserCount)}명 기준</small>}</div>
      </div>
    </div>
  </section>;
}

export default function GameDetailView({ gameId }: { gameId: string }) {
  const router = useRouter();
  const auth = useAuth();
  const [game, setGame] = useState<GameDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadGame() {
      setLoading(true);
      setError("");
      try {
        const response = await fetch(`/api/games/${gameId}`, { signal: controller.signal });
        const body = (await response.json()) as DetailResponse;
        if (!response.ok || !body.data) {
          throw new Error(response.status === 404 ? "게임 정보를 찾지 못했어요." : body.msg || "게임 정보를 불러오지 못했어요.");
        }
        setGame(body.data);
      } catch (reason) {
        if (!controller.signal.aborted) {
          setGame(null);
          setError(reason instanceof Error ? reason.message : "게임 정보를 불러오지 못했어요.");
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadGame();
    return () => controller.abort();
  }, [gameId, retry]);

  return <>
    <header className="header"><div className="header-inner">
      <Link className="logo" href="/" aria-label="GameLog 홈">GameLog<span className="lime">.</span></Link>
      <nav aria-label="현재 위치"><Link href="/">게임 탐색</Link><span className={styles.currentNav} aria-current="page">게임 상세</span></nav>
      <AuthNav/>
      <span className={`header-note ${styles.headerNote}`}>PLAY. RECORD. DISCOVER.</span>
    </div></header>

    <main className={styles.main} aria-busy={loading}>
      <Link className={styles.backLink} href="/">← 게임 탐색으로 돌아가기</Link>

      {loading ? <div className={styles.hero} aria-label="게임 정보를 불러오는 중" role="status">
        <div className={styles.skeletonCover} />
        <div className={styles.loadingCopy}><div className={styles.skeletonLine} /><div className={styles.skeletonLine} /><p>게임 정보를 불러오는 중...</p></div>
      </div> : error ? <div className={styles.errorState} role="alert">
        <span className={styles.eyebrow}>GAME DETAILS</span>
        <h1>게임 정보를 표시할 수 없어요.</h1>
        <p>{error}</p>
        <button className="apply-button" onClick={() => setRetry(value => value + 1)}>다시 시도 ↗</button>
      </div> : game ? <section className={styles.hero} aria-labelledby="game-title">
        <div className={styles.posterColumn}>
          <div className={styles.cover}><GameCover key={game.id} game={game} /></div>
          <MyGameLog
            gameId={Number(gameId)}
            platforms={game.platforms ?? []}
            accessToken={auth.accessToken ?? undefined}
            onLoginRequired={() => router.push(`/login?next=/games/${gameId}`)}
          />
        </div>
        <div className={styles.content}>
          <span className={styles.eyebrow}><span className="dot" /> GAME DETAILS</span>
          <h1 id="game-title">{game.title}</h1>
          <div className={styles.meta}>
            {game.developer && <span>by <strong>{game.developer}</strong></span>}
            <span className={styles.releaseDate}>{game.releaseDate ? <><strong>Released</strong><time dateTime={game.releaseDate}>{formatReleaseDate(game.releaseDate)}</time></> : "출시일 미정"}</span>
          </div>
          <p className={styles.description}>{game.description || "등록된 게임 소개가 없습니다."}</p>
          <div className={styles.infoRows}>
            <div className={styles.infoRow}><span>장르</span><div className={styles.tags}>{game.genres?.length ? game.genres.map(genre => <span key={genre.id}>{genre.name}</span>) : <span className={styles.noInfo}>정보 없음</span>}</div></div>
            <div className={styles.infoRow}><span>플랫폼</span><div className={styles.tags}>{game.platforms?.length ? game.platforms.map(platform => <span key={platform.id}>{platform.name}</span>) : <span className={styles.noInfo}>정보 없음</span>}</div></div>
            {game.series?.length > 0 && <div className={styles.infoRow}><span>시리즈</span><div className={styles.tags}>{game.series.map(series => <span key={series.id}>{series.name}</span>)}</div></div>}
          </div>
          <GameStats statistics={game.statistics} />
        </div>
      </section> : null}
      {!loading && !error && game && <>
        <RelatedGames key={gameId} gameId={gameId} />
        <GameReviews gameId={gameId} />
      </>}
    </main>
  </>;
}
