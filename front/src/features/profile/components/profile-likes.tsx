"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { getLikedGames, ProfileApiError } from "@/features/profile/api";
import type { UserGame, UserGameLibraryResponse } from "@/features/profile/types";
import { coverUrl } from "@/features/games/model";
import styles from "./profile.module.css";

const PAGE_SIZE = 20;
type LikesSort = "RECENT_PLAYED" | "TITLE";

const PLAY_STATUS_LABEL: Record<string, string> = {
  PLAYED: "플레이함",
  COMPLETED: "플레이 완료",
  RETIRED: "끝냄",
  SHELVED: "잠시 보류",
  DROPPED: "플레이 포기",
};

function gameStatus(game: UserGame) {
  if (game.playStatus) return PLAY_STATUS_LABEL[game.playStatus] ?? game.playStatus;
  if (game.playing) return "플레이 중";
  if (game.backlog) return "플레이 예정";
  if (game.wishlist) return "위시리스트";
  return null;
}

function LikedGameCard({ game }: { game: UserGame }) {
  const [coverFailed, setCoverFailed] = useState(false);
  const src = coverUrl(game.coverImageUrl);
  const status = gameStatus(game);

  return (
    <article className={styles.profileLikeCard}>
      <Link href={`/games/${game.gameId}`} className={styles.profileLikeCoverLink}>
        {src && !coverFailed ? (
          <Image
            className={styles.profileLikeCover}
            src={src}
            alt={`${game.title} 커버`}
            width={210}
            height={294}
            unoptimized
            onError={() => setCoverFailed(true)}
          />
        ) : (
          <span className={styles.profileLikeCoverFallback}>NO COVER</span>
        )}
        <span className={styles.profileLikeHeart} aria-label="좋아하는 게임">♥</span>
      </Link>
      <div className={styles.profileLikeCardBody}>
        <h3><Link href={`/games/${game.gameId}`}>{game.title}</Link></h3>
        {status ? <span className={styles.profileLikeStatus}>{status}</span> : <span className={styles.profileLikeOnly}>좋아하는 게임</span>}
      </div>
    </article>
  );
}

export default function ProfileLikes({ accessToken }: { accessToken: string }) {
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState<LikesSort>("RECENT_PLAYED");
  const [result, setResult] = useState<UserGameLibraryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    async function load() {
      setLoading(true);
      setError("");
      try {
        const response = await getLikedGames(accessToken, page, PAGE_SIZE, sort);
        if (active) setResult(response);
      } catch (cause) {
        if (!active) return;
        setError(
          cause instanceof ProfileApiError || cause instanceof Error
            ? cause.message
            : "좋아하는 게임을 불러오지 못했습니다.",
        );
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();
    return () => {
      active = false;
    };
  }, [accessToken, page, sort]);

  return (
    <section className={styles.profileLikesSection} aria-labelledby="profile-likes-title">
      <div className={styles.profileLikesToolbar}>
        <div>
          <span className={styles.sectionEyebrow}>MY LIKES</span>
          <h2 id="profile-likes-title"><strong>{result?.totalElements ?? 0}</strong>개의 좋아하는 게임</h2>
          <p>게임 상세에서 하트를 누른 게임을 모았어요.</p>
        </div>
        <label className={styles.profileReviewsSort}>
          <span>정렬</span>
          <select
            value={sort}
            onChange={(event) => {
              setSort(event.target.value as LikesSort);
              setPage(0);
            }}
          >
            <option value="RECENT_PLAYED">최근 기록순</option>
            <option value="TITLE">제목순</option>
          </select>
        </label>
      </div>

      {loading ? <p className={styles.loading}>좋아하는 게임을 불러오는 중…</p> : null}
      {!loading && error ? <div className={styles.errorBox}>{error}</div> : null}
      {!loading && !error && result?.userGames.length === 0 ? (
        <div className={styles.emptyBox}>아직 좋아하는 게임이 없습니다.</div>
      ) : null}

      {!loading && !error && result?.userGames.length ? (
        <div className={styles.profileLikesGrid}>
          {result.userGames.map((game) => <LikedGameCard key={game.gameId} game={game} />)}
        </div>
      ) : null}

      {!loading && !error && result && result.totalPages > 1 ? (
        <nav className={styles.profileLikesPagination} aria-label="좋아하는 게임 페이지 이동">
          <button type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>‹ 이전</button>
          <span>{page + 1} / {result.totalPages}</span>
          <button type="button" disabled={page + 1 >= result.totalPages} onClick={() => setPage((value) => value + 1)}>다음 ›</button>
        </nav>
      ) : null}
    </section>
  );
}
