"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { coverUrl, type GameDetail, type GameStatistics } from "@/lib/games";
import styles from "./game-detail-view.module.css";

type DetailResponse = { data?: GameDetail; msg?: string };
const numberFormat = new Intl.NumberFormat("ko-KR");

function GameCover({ game }: { game: GameDetail }) {
  const [broken, setBroken] = useState(false);
  const url = coverUrl(game.coverImageUrl);

  if (!url || broken) {
    return <div className={styles.coverFallback}><span>{game.title}</span><small>커버 준비 중</small></div>;
  }

  // eslint-disable-next-line @next/next/no-img-element
  return <img src={url} alt={`${game.title} 커버`} onError={() => setBroken(true)} />;
}

function GameStats({ statistics }: { statistics: GameStatistics }) {
  const distribution = statistics.ratingDistribution ?? [];
  const maxCount = Math.max(1, ...distribution.map(item => item.count));
  const statuses = [
    { label: "플레이 완료", english: "Played", count: statistics.playedCount },
    { label: "플레이 중", english: "Playing", count: statistics.playingCount },
    { label: "플레이 예정", english: "Backlog", count: statistics.backlogCount },
    { label: "위시리스트", english: "Wishlist", count: statistics.wishlistCount },
  ];

  return <section className={styles.statsSection} aria-labelledby="stats-title">
    <div className={styles.sectionHeading}>
      <div><span className={styles.eyebrow}>THE COMMUNITY AT A GLANCE</span><h2 id="stats-title">이 게임의 기록</h2></div>
      <p>GameLog 사용자들의 기록을 모았어요.</p>
    </div>
    <div className={styles.statsGrid}>
      <div className={`${styles.statCard} ${styles.ratingCard}`}>
        <span className={styles.cardLabel}>GAMELOG RATING</span>
        <div className={styles.ratingValue}>{statistics.averageRating.toFixed(1)}<small>/ 5.0</small></div>
        <p className={styles.cardHint}>{statistics.reviewCount === 0 ? "아직 등록된 리뷰가 없어요." : `${numberFormat.format(statistics.reviewCount)}개의 리뷰 기준`}</p>
        <div className={styles.distribution} role="img" aria-label={distribution.map(item => `${item.rating}점 ${item.count}명`).join(", ") || "평점 데이터 없음"}>
          {distribution.map(item => <div className={styles.distributionItem} key={item.rating} aria-hidden="true">
            <span className={styles.barTrack}><span className={styles.barFill} style={{ height: `${item.count / maxCount * 100}%` }} /></span>
            <span className={styles.barLabel}>{item.rating.toFixed(1)}</span>
          </div>)}
        </div>
      </div>
      <div className={styles.statCard}>
        <span className={styles.cardLabel}>PLAY STATUS</span>
        <h3>플레이 현황</h3>
        <div className={styles.statusList}>{statuses.map(status => <div className={styles.statusRow} key={status.english}>
          <span>{status.label}<small>{status.english}</small></span><strong>{numberFormat.format(status.count)}</strong>
        </div>)}</div>
      </div>
      <div className={styles.statCard}>
        <span className={styles.cardLabel}>COMMUNITY & TIME</span>
        <h3>함께 남긴 기록</h3>
        <div className={styles.metricList}>
          <div><span>리뷰</span><strong>{numberFormat.format(statistics.reviewCount)}<small>개</small></strong></div>
          <div><span>좋아요</span><strong>{numberFormat.format(statistics.likeCount)}<small>명</small></strong></div>
          <div><span>평균 플레이 시간</span><strong>{statistics.playTimeUserCount === 0 ? "—" : statistics.averagePlayTimeHours.toFixed(1)}<small>{statistics.playTimeUserCount === 0 ? "기록 없음" : "시간"}</small></strong></div>
        </div>
        <p className={styles.timeHint}>플레이 시간 기록 {numberFormat.format(statistics.playTimeUserCount)}명 기준</p>
      </div>
    </div>
  </section>;
}

export default function GameDetailView({ gameId }: { gameId: string }) {
  const [game, setGame] = useState<GameDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const [expanded, setExpanded] = useState(false);

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
          {game.igdbRating != null && <p className={styles.igdbRating}>IGDB 평점 <strong>{game.igdbRating.toFixed(1)}</strong><span> / 100</span></p>}
        </div>
        <div className={styles.content}>
          <span className={styles.eyebrow}><span className="dot" /> GAME DETAILS</span>
          <h1 id="game-title">{game.title}</h1>
          <div className={styles.meta}>
            {game.developer && <span>by <strong>{game.developer}</strong></span>}
            <span className={styles.releaseDate}>{game.releaseDate || "출시일 미정"}</span>
          </div>
          <p className={styles.description}>{game.description ? expanded || game.description.length <= 320 ? game.description : `${game.description.slice(0, 320).trimEnd()}…` : "등록된 게임 소개가 없습니다."}</p>
          {game.description && game.description.length > 320 && <button className={styles.moreButton} onClick={() => setExpanded(value => !value)} aria-expanded={expanded}>{expanded ? "접기 ↑" : "더 보기 ↓"}</button>}
          <div className={styles.infoRows}>
            <div className={styles.infoRow}><span>장르</span><div className={styles.tags}>{game.genres?.length ? game.genres.map(genre => <span key={genre.id}>{genre.name}</span>) : <span className={styles.noInfo}>정보 없음</span>}</div></div>
            <div className={styles.infoRow}><span>플랫폼</span><div className={styles.tags}>{game.platforms?.length ? game.platforms.map(platform => <span key={platform.id}>{platform.name}</span>) : <span className={styles.noInfo}>정보 없음</span>}</div></div>
            {game.series?.length > 0 && <div className={styles.infoRow}><span>시리즈</span><div className={styles.tags}>{game.series.map(series => <span key={series.id}>{series.name}</span>)}</div></div>}
          </div>
          <GameStats statistics={game.statistics} />
        </div>
      </section> : null}
    </main>
  </>;
}
