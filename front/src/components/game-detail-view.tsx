"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { coverUrl, type GameDetail } from "@/lib/games";
import styles from "./game-detail-view.module.css";

type DetailResponse = { data?: GameDetail; msg?: string };

function GameCover({ game }: { game: GameDetail }) {
  const [broken, setBroken] = useState(false);
  const url = coverUrl(game.coverImageUrl);

  if (!url || broken) {
    return <div className={styles.coverFallback}><span>{game.title}</span><small>커버 준비 중</small></div>;
  }

  // eslint-disable-next-line @next/next/no-img-element
  return <img src={url} alt={`${game.title} 커버`} onError={() => setBroken(true)} />;
}

export default function GameDetailView({ gameId }: { gameId: string }) {
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
        <div className={styles.cover}><GameCover key={game.id} game={game} /></div>
        <div className={styles.content}>
          <span className={styles.eyebrow}><span className="dot" /> GAME DETAILS</span>
          <h1 id="game-title">{game.title}</h1>
          <div className={styles.meta}>
            <span>{game.releaseDate || "출시일 미정"}</span>
            {game.developer && <><span className={styles.separator} aria-hidden="true">/</span><span>{game.developer}</span></>}
          </div>
          <div className={styles.rule} />
          <p className={styles.description}>{game.description || "등록된 게임 소개가 없습니다."}</p>
        </div>
      </section> : null}
    </main>
  </>;
}
