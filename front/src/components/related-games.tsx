"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { coverUrl, type RelatedGame } from "@/lib/games";
import styles from "./related-games.module.css";

type RelatedResponse = { data?: RelatedGame[]; msg?: string };
type QueryState =
  | { status: "loading" }
  | { status: "success"; games: RelatedGame[] }
  | { status: "error" };

function RecommendationCard({ game }: { game: RelatedGame }) {
  const [broken, setBroken] = useState(false);
  const imageUrl = coverUrl(game.coverImageUrl);
  const genres = game.genres ?? [];

  return <li>
    <Link className={styles.card} href={`/games/${game.id}`}>
      <div className={styles.cover}>
        {imageUrl && !broken ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={imageUrl} alt="" loading="lazy" onError={() => setBroken(true)} />
        ) : <div className={styles.fallback}><span>{game.title}</span><small>커버 준비 중</small></div>}
        <span className={styles.openHint} aria-hidden="true">↗</span>
      </div>
      <div className={styles.cardInfo}>
        <h3>{game.title}</h3>
        <div className={styles.genres}>
          {genres.slice(0, 2).map(genre => <span key={genre.id} title={genre.name}>{genre.name}</span>)}
          {genres.length > 2 && <span title={genres.slice(2).map(genre => genre.name).join(", ")}>+{genres.length - 2}</span>}
        </div>
        {/* TODO: 추천 결과 확인 후 임시 점수 표시 제거 */}
        <p className={styles.debugScore}>임시: 점수 {game.recommendationScore.toFixed(3)}점</p>
        <span className={styles.detailLink}>게임 살펴보기 <span aria-hidden="true">↗</span></span>
      </div>
    </Link>
  </li>;
}

export default function RelatedGames({ gameId }: { gameId: string }) {
  const [state, setState] = useState<QueryState>({ status: "loading" });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadRelatedGames() {
      try {
        const response = await fetch(`/api/games/${gameId}/related`, {
          signal: controller.signal,
          cache: "no-store",
        });
        const body = await response.json() as RelatedResponse;
        if (!response.ok || !Array.isArray(body.data)) throw new Error("추천 조회 실패");
        if (!controller.signal.aborted) {
          // 서버의 추천 순서를 유지하고 최대 5개만 표시한다.
          setState({ status: "success", games: body.data.slice(0, 5) });
        }
      } catch {
        if (!controller.signal.aborted) setState({ status: "error" });
      }
    }

    void loadRelatedGames();
    return () => controller.abort();
  }, [gameId, attempt]);

  function retry() {
    setState({ status: "loading" });
    setAttempt(value => value + 1);
  }

  return <section className={styles.section} aria-labelledby="related-games-title">
    <div className={styles.heading}>
      <div>
        <span className={styles.eyebrow}>DISCOVER YOUR NEXT GAME</span>
        <h2 id="related-games-title">이 게임이 좋았다면</h2>
      </div>
      <p>함께 즐겨볼 만한 게임을 만나보세요.</p>
    </div>
    {state.status === "loading" ? <div role="status" aria-label="연관 추천 게임을 불러오는 중">
      <div className={styles.grid} aria-hidden="true">
        {Array.from({ length: 5 }, (_, index) => <div className={styles.skeleton} key={index}>
          <div className={styles.skeletonCover} /><div className={styles.skeletonTitle} />
        </div>)}
      </div>
    </div> : state.status === "error" ? <div className={styles.state} role="alert">
      <p>추천 게임을 불러오지 못했어요.</p>
      <button type="button" onClick={retry}>다시 시도 <span aria-hidden="true">↗</span></button>
    </div> : state.games.length === 0 ? <div className={styles.state} role="status">
      <p>아직 추천할 게임이 없어요.</p>
      <span>다른 게임도 둘러보며 취향에 맞는 게임을 찾아보세요.</span>
    </div> : <ul className={styles.grid}>
      {state.games.map(game => <RecommendationCard key={game.id} game={game} />)}
    </ul>}
  </section>;
}
