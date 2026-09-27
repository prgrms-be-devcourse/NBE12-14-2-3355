"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useAuth, AuthApiError } from "@/features/auth/auth-context";
import { getPersonalizedGames, type PersonalizedGame } from "@/features/recommendations/api";
import { coverUrl } from "@/features/games/model";
import ImageWithFallback from "@/components/ui/image-with-fallback";
import GameCoverFallback from "@/components/ui/game-cover-fallback";
import styles from "./related-games.module.css";
import personalizedStyles from "./personalized-games.module.css";

// 메인 추천 영역에서 사용할 커버·장르·임시 추천 점수 카드
function RecommendationCard({ game }: { game: PersonalizedGame }) {
  const genres = game.genres ?? [];

  return <li>
    <Link className={styles.card} href={`/games/${game.id}`}>
      <div className={styles.cover}>
        <ImageWithFallback
          src={coverUrl(game.coverImageUrl)} alt=""
          fallback={<GameCoverFallback title={game.title} />}
        />
        <span className={styles.openHint} aria-hidden="true">↗</span>
      </div>
      <div className={styles.cardInfo}>
        <h3>{game.title}</h3>
        <div className={styles.genres}>
          {genres.slice(0, 2).map(genre => <span key={genre.id} title={genre.name}>{genre.name}</span>)}
          {genres.length > 2 && <span title={genres.slice(2).map(genre => genre.name).join(", ")}>+{genres.length - 2}</span>}
        </div>
        {/* TODO: 추천 결과 확인 후 임시 recommendationScore 표시 제거 */}
        <p className={personalizedStyles.debugScore}>임시: recommendationScore {game.recommendationScore.toFixed(3)}점</p>
        <span className={styles.detailLink}>게임 살펴보기 <span aria-hidden="true">↗</span></span>
      </div>
    </Link>
  </li>;
}

function LoadingCards() {
  return <div role="status" aria-label="맞춤 추천 게임을 불러오는 중">
    <div className={styles.grid} aria-hidden="true">
      {Array.from({ length: 5 }, (_, index) => <div className={styles.skeleton} key={index}>
        <div className={styles.skeletonCover} /><div className={styles.skeletonTitle} />
      </div>)}
    </div>
  </div>;
}

function LoginGuide() {
  return <div className={styles.state}>
    <p>로그인하고 나만의 추천 게임을 만나보세요.</p>
    <Link className={personalizedStyles.action} href="/login?next=/">로그인하기 ↗</Link>
  </div>;
}

type QueryState =
  | { status: "loading" }
  | { status: "success"; games: PersonalizedGame[] }
  | { status: "error" }
  | { status: "unauthorized" };

// 인증 정보 또는 재시도가 바뀌면 새로 마운트해 이전 요청 결과를 제거
function RecommendationResults({ accessToken }: { accessToken: string }) {
  const [state, setState] = useState<QueryState>({ status: "loading" });

  useEffect(() => {
    const controller = new AbortController();
    getPersonalizedGames(accessToken, controller.signal).then(games => {
      if (!controller.signal.aborted) setState({ status: "success", games });
    }).catch(error => {
      if (!controller.signal.aborted) {
        setState({ status: error instanceof AuthApiError && error.status === 401 ? "unauthorized" : "error" });
      }
    });
    return () => controller.abort();
  }, [accessToken]);

  if (state.status === "loading") return <LoadingCards />;
  if (state.status === "unauthorized") return <LoginGuide />;
  if (state.status === "error") return <p role="alert">추천 게임을 불러오지 못했어요. 아래 버튼으로 다시 시도해 주세요.</p>;
  if (!state.games.length) return <div className={styles.state} role="status">
    <p>게임을 기록하고 나만의 추천을 받아보세요.</p>
    <Link className={personalizedStyles.action} href="/games">기록할 게임 찾아보기 ↗</Link>
  </div>;
  return <ul className={styles.grid}>{state.games.map(game => <RecommendationCard key={game.id} game={game} />)}</ul>;
}

// 인증 상태에 따라 안내 또는 로그인 사용자의 추천 결과 표시
export default function PersonalizedGames() {
  const auth = useAuth();
  const [attempt, setAttempt] = useState(0);

  return <section className={personalizedStyles.section} aria-labelledby="personalized-games-title">
    <div className={styles.heading}>
      <div><span className={styles.eyebrow}>PICKED FOR YOU</span><h2 id="personalized-games-title">당신을 위한 추천 게임</h2></div>
      <p>당신의 취향과 기록에서 찾은 다음 게임.</p>
    </div>
    {auth.status === "loading" ? <LoadingCards /> : auth.status === "authenticated" && auth.user && auth.accessToken ? <>
      <RecommendationResults key={`${auth.user.id}:${auth.accessToken}:${attempt}`} accessToken={auth.accessToken} />
      <button className={personalizedStyles.refresh} type="button" onClick={() => setAttempt(value => value + 1)}>추천 다시 불러오기 ↻</button>
    </> : <LoginGuide />}
  </section>;
}
