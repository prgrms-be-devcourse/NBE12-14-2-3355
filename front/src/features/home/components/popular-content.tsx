"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { coverUrl } from "@/features/games/model";
import {
  getPopularGames,
  getPopularReviews,
  type PopularGame,
  type PopularReview,
} from "@/features/home/api";
import gameStyles from "@/features/recommendations/components/related-games.module.css";
import reviewStyles from "@/features/reviews/components/game-reviews.module.css";
import styles from "./popular-content.module.css";

const numberFormat = new Intl.NumberFormat("ko-KR");
const dateFormat = new Intl.DateTimeFormat("ko-KR", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
});

type QueryState<T> =
  | { status: "loading" }
  | { status: "success"; data: T[] }
  | { status: "error"; message: string };

function usePopularContent<T>(loader: (signal: AbortSignal) => Promise<T[]>) {
  const [attempt, setAttempt] = useState(0);
  const [state, setState] = useState<QueryState<T>>({ status: "loading" });

  useEffect(() => {
    const controller = new AbortController();
    setState({ status: "loading" });

    loader(controller.signal)
      .then(data => {
        if (!controller.signal.aborted) setState({ status: "success", data });
      })
      .catch((reason: unknown) => {
        if (!controller.signal.aborted) {
          setState({
            status: "error",
            message: reason instanceof Error ? reason.message : "인기 콘텐츠를 불러오지 못했어요.",
          });
        }
      });

    return () => controller.abort();
  }, [attempt, loader]);

  return { state, retry: () => setAttempt(value => value + 1) };
}

function SectionHeading({ type }: { type: "games" | "reviews" }) {
  const games = type === "games";

  return <div className={styles.heading}>
    <div>
      <span>{games ? "COMMUNITY PICKS" : "MOST LOVED STORIES"}</span>
      <h2 id={`popular-${type}-title`}>{games ? "Popular Games" : "Popular Reviews"}</h2>
    </div>
    <p>{games ? "GameLog 사용자들이 가장 많이 좋아한 게임" : "사용자들에게 가장 많은 공감을 받은 리뷰"}</p>
  </div>;
}

function LoadingState({ type }: { type: "games" | "reviews" }) {
  if (type === "reviews") {
    return <div className={reviewStyles.loading} role="status">인기 리뷰를 불러오는 중...</div>;
  }

  return <div role="status" aria-label="인기 게임을 불러오는 중">
    <div className={gameStyles.grid} aria-hidden="true">
      {Array.from({ length: 5 }, (_, index) => <div className={gameStyles.skeleton} key={index}>
        <div className={gameStyles.skeletonCover} /><div className={gameStyles.skeletonTitle} />
      </div>)}
    </div>
  </div>;
}

function StateMessage({ children, retry, type }: { children: string; retry?: () => void; type: "games" | "reviews" }) {
  const stateClass = type === "games" ? gameStyles.state : reviewStyles.emptyState;

  return <div className={stateClass} role={retry ? "alert" : "status"}>
    {type === "reviews" ? <h3>{children}</h3> : <p>{children}</p>}
    {retry && <button type="button" onClick={retry}>다시 불러오기 ↻</button>}
  </div>;
}

function PopularGameCard({ game }: { game: PopularGame }) {
  const [broken, setBroken] = useState(false);
  const imageUrl = coverUrl(game.coverImageUrl);
  const genres = game.genres ?? [];

  return <li>
    <Link className={gameStyles.card} href={`/games/${game.gameId}`}>
      <div className={gameStyles.cover}>
        {imageUrl && !broken ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={imageUrl} alt={`${game.title} 커버`} loading="lazy" onError={() => setBroken(true)} />
        ) : <div className={gameStyles.fallback}><span>{game.title}</span><small>커버 준비 중</small></div>}
        <span className={gameStyles.openHint} aria-hidden="true">↗</span>
      </div>
      <div className={gameStyles.cardInfo}>
        <h3>{game.title}</h3>
        <div className={gameStyles.genres} aria-label="게임 장르">
          {genres.slice(0, 2).map(genre => <span key={genre.id} title={genre.name}>{genre.name}</span>)}
          {genres.length > 2 && <span title={genres.slice(2).map(genre => genre.name).join(", ")}>+{genres.length - 2}</span>}
        </div>
        <p className={styles.gameLike}><span aria-hidden="true">♥</span> 좋아요 {numberFormat.format(game.likeCount)}</p>
        <span className={gameStyles.detailLink}>게임 살펴보기 <span aria-hidden="true">↗</span></span>
      </div>
    </Link>
  </li>;
}

function PopularReviewCard({ review }: { review: PopularReview }) {
  const [revealed, setRevealed] = useState(false);
  const [brokenCover, setBrokenCover] = useState(false);
  const gameCover = coverUrl(review.gameCoverImageUrl);
  const nickname = review.nickname?.trim() || `플레이어 ${review.userId}`;
  const hidden = review.spoiler && !revealed;
  const createdDate = new Date(review.createdDate);
  const formattedDate = Number.isNaN(createdDate.getTime()) ? review.createdDate.slice(0, 10) : dateFormat.format(createdDate);
  const avatarStyle = review.profileImageUrl
    ? { backgroundImage: `url(${JSON.stringify(review.profileImageUrl)})` }
    : undefined;

  return <article className={reviewStyles.card}>
    <div className={styles.reviewHeader}>
      <div className={reviewStyles.reviewIdentity}>
        <span
          className={`${reviewStyles.avatar} ${review.profileImageUrl ? reviewStyles.avatarImage : ""}`}
          style={avatarStyle}
          aria-hidden="true"
        >
          {!review.profileImageUrl && nickname.slice(0, 1).toUpperCase()}
        </span>
        <div className={reviewStyles.reviewerDetails}>
          <div className={reviewStyles.reviewerName}>
            <Link className={reviewStyles.reviewerProfileLink} href={`/profile/${review.userId}`}>{nickname}</Link><span>님이 리뷰를 남겼어요</span>
          </div>
          <div className={reviewStyles.reviewerMeta}>
            {review.rating == null
              ? <span className={reviewStyles.noRating}>별점 없음</span>
              : <span className={reviewStyles.rating}><span aria-hidden="true">★</span><strong>{review.rating.toFixed(1)}</strong><small>/ 5.0</small></span>}
          </div>
        </div>
      </div>
      <Link className={styles.reviewGame} href={`/games/${review.gameId}`}>
        <span className={styles.reviewGameCover}>
          {gameCover && !brokenCover ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={gameCover} alt={`${review.gameTitle} 커버`} loading="lazy" onError={() => setBrokenCover(true)} />
          ) : <span aria-hidden="true">GL</span>}
        </span>
        <span className={styles.reviewGameInfo}>
          <small>REVIEW OF</small>
          <strong>{review.gameTitle}</strong>
        </span>
        <span className={styles.reviewGameArrow} aria-hidden="true">↗</span>
      </Link>
      <time dateTime={review.createdDate}>{formattedDate}</time>
    </div>
    <div className={reviewStyles.reviewBody}>
      <div className={styles.reviewCopy}>
        {hidden && <div className={reviewStyles.spoilerCover}>
          <span>스포일러가 포함된 리뷰예요.</span>
          <button type="button" onClick={() => setRevealed(true)}>내용 보기</button>
        </div>}
        <p className={hidden ? reviewStyles.blurred : undefined} aria-hidden={hidden}>{review.content}</p>
      </div>
    </div>
    <div className={reviewStyles.cardActions}>
      <span className={`${reviewStyles.likeCount} ${styles.staticLike}`} aria-label={`좋아요 ${review.likeCount}개`}>
        <span aria-hidden="true">♥</span>{numberFormat.format(review.likeCount)}
      </span>
    </div>
  </article>;
}

export function PopularGamesSection() {
  const { state, retry } = usePopularContent(getPopularGames);

  return <section className={styles.section} aria-labelledby="popular-games-title">
    <SectionHeading type="games" />
    {state.status === "loading" ? <LoadingState type="games" />
      : state.status === "error" ? <StateMessage retry={retry} type="games">{state.message}</StateMessage>
      : state.data.length === 0 ? <StateMessage type="games">아직 좋아요를 받은 게임이 없어요.</StateMessage>
      : <ul className={gameStyles.grid}>{state.data.map(game => <PopularGameCard game={game} key={game.gameId} />)}</ul>}
  </section>;
}

export function PopularReviewsSection() {
  const { state, retry } = usePopularContent(getPopularReviews);

  return <section className={styles.section} aria-labelledby="popular-reviews-title">
    <SectionHeading type="reviews" />
    {state.status === "loading" ? <LoadingState type="reviews" />
      : state.status === "error" ? <StateMessage retry={retry} type="reviews">{state.message}</StateMessage>
      : state.data.length === 0 ? <StateMessage type="reviews">아직 좋아요를 받은 리뷰가 없어요.</StateMessage>
      : <div className={reviewStyles.list}>{state.data.map(review => <PopularReviewCard review={review} key={review.reviewId} />)}</div>}
  </section>;
}
