"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import GameSearch from "@/features/games/components/game-search";
import SelectDropdown from "@/components/ui/select-dropdown";
import { acceptRefreshedToken } from "@/features/auth/api";
import { coverUrl, type Option } from "@/features/games/model";
import styles from "./profile.module.css";

const sortOptions: { value: string; label: string }[] = [
  { value: "RECENT_PLAYED", label: "최근 플레이순" },
  { value: "RATING", label: "별점순" },
  { value: "TITLE", label: "가나다순" },
  { value: "PLAY_TIME", label: "플레이타임 순" },
];

type LibraryGame = { gameId: number; title: string; coverImageUrl: string | null; playStatus: string | null; playing: boolean; backlog: boolean; wishlist: boolean; liked: boolean };
type LibraryResult = { totalPages: number; userGames: LibraryGame[] };
const statuses = ["ALL", "PLAYED", "PLAYING", "BACKLOG", "WISHLIST"] as const;
const labels = { ALL: "All", PLAYED: "Played", PLAYING: "Playing", BACKLOG: "Backlog", WISHLIST: "Wishlist" };

// 프로필 Games 탭의 라이브러리 목록
export default function LibraryGames({ accessToken, userId }: { accessToken?: string, userId?: number; }) {
  const router = useRouter();
  const [status, setStatus] = useState<typeof statuses[number]>("ALL");
  const [sort, setSort] = useState("RECENT_PLAYED");
  const [input, setInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [filters, setFilters] = useState<{ genres: number[]; platforms: number[] }>({ genres: [], platforms: [] });
  const [options, setOptions] = useState<{ genres: Option[]; platforms: Option[] }>({ genres: [], platforms: [] });
  const [optionsError, setOptionsError] = useState("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<LibraryResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const endpoint =
    userId !== undefined
      ? `/api/library/games/profile/${userId}/games`
      : `/api/library/games`; 

  useEffect(() => {
    const controller = new AbortController();
    fetch("/api/games/filters", { signal: controller.signal }).then(async response => {
      const body = await response.json();
      if (!response.ok) throw new Error(body.msg);
      setOptions(body.data); setOptionsError("");
    }).catch(() => { if (!controller.signal.aborted) setOptionsError("필터 목록을 불러오지 못했어요."); });
    return () => controller.abort();
  }, [retry]);

  useEffect(() => {
    const controller = new AbortController();
    const query = new URLSearchParams({ status, sort, page: String(page), size: "70" });
    if (keyword) query.set("keyword", keyword);
    filters.genres.forEach(id => query.append("genreIds", String(id)));
    filters.platforms.forEach(id => query.append("platformIds", String(id)));
    async function load() {
      setLoading(true); setError("");
      try {
        const headers: HeadersInit = {};

        if (accessToken) {
          headers.Authorization = `Bearer ${accessToken}`;
        }

        const response = await fetch(
          `${endpoint}?${query}`,
          {
            headers,
            signal: controller.signal,
            cache: "no-store",
          }
        );
        // const response = await fetch(`/api/library/games?${query}`, { headers: { Authorization: `Bearer ${accessToken}` }, signal: controller.signal, cache: "no-store" });
        acceptRefreshedToken(response);
        const body = await response.json();
        if (!response.ok) throw new Error(body.msg || "게임 목록을 불러오지 못했어요.");
        if (!controller.signal.aborted) setResult(body.data);
      } catch (reason) {
        if (!controller.signal.aborted) setError(reason instanceof Error ? reason.message : "게임 목록을 불러오지 못했어요.");
      } finally { if (!controller.signal.aborted) setLoading(false); }
    }
    void load();
    return () => controller.abort();
  }, [accessToken, status, sort, keyword, filters, page, retry, endpoint]);

  function reset() { setStatus("ALL"); setSort("RECENT_PLAYED"); setInput(""); setKeyword(""); setFilters({ genres: [], platforms: [] }); setPage(0); }

  return <section aria-label="내 게임 라이브러리">
    <h2 className={styles.contentTitle}>{userId === undefined ? "내 게임 라이브러리" : "게임 라이브러리"}</h2>
    <div className={styles.statusTabs} aria-label="플레이 상태">{statuses.map(value => <button key={value} type="button" aria-pressed={status === value} onClick={() => { setStatus(status === value ? "ALL" : value); setPage(0); }}>{labels[value]}</button>)}</div>
    <div className={styles.libraryToolbar}>
      <div className={styles.librarySearch}>
        <GameSearch value={input} demo={false} libraryAccessToken={accessToken} libraryUserId={userId} onChange={setInput}
          onSearch={() => { setKeyword(input.trim()); setPage(0); }}
          onSelect={game => router.push(`/games/${game.id}`)} />
      </div>
      <label>정렬 <SelectDropdown ariaLabel="정렬" value={sort} options={sortOptions} onChange={value => { setSort(value); setPage(0); }} /></label>
    </div>
    <div className={styles.libraryFilters}>{(["platforms", "genres"] as const).map(type => <details key={type}><summary>{type === "genres" ? "장르" : "플랫폼"} · {filters[type].length}개 선택</summary><fieldset><legend>여러 개 선택 가능</legend>{options[type].map(option => <label key={option.id}><input type="checkbox" checked={filters[type].includes(option.id)} onChange={() => { setFilters(previous => ({ ...previous, [type]: previous[type].includes(option.id) ? previous[type].filter(id => id !== option.id) : [...previous[type], option.id] })); setPage(0); }} />{option.name}</label>)}{!options[type].length && <p>선택 가능한 항목이 없습니다.</p>}</fieldset></details>)}<button type="button" className={styles.linkBtn} onClick={reset}>조건 초기화</button></div>
    {optionsError && <p className={styles.error} role="alert">{optionsError} <button type="button" onClick={() => setRetry(value => value + 1)}>다시 시도</button></p>}
    {keyword && <p className={styles.searchSummary}>검색어: {keyword}</p>}
    {loading ? <p className={styles.loading} role="status">게임을 불러오는 중…</p> : error ? <div className={styles.emptyBox} role="alert">{error}<button type="button" className={styles.linkBtn} onClick={() => setRetry(value => value + 1)}>다시 시도</button></div> : !result?.userGames.length ? <div className={styles.emptyBox}>조건에 맞는 게임이 없어요. 검색·필터 조건을 바꾸거나 게임을 등록해 주세요.</div> : <>
      <div className={styles.libraryGrid}>{result.userGames.map(game => <Link key={game.gameId} href={`/games/${game.gameId}`} className={styles.libraryCard}>
        <LibraryCover game={game} />
        <strong>{game.title}</strong><div className={styles.libraryBadges}>{game.playStatus && <span>Played</span>}{game.playing && <span>Playing</span>}{game.backlog && <span>Backlog</span>}{game.wishlist && <span>Wishlist</span>}{game.liked && <span>♥</span>}</div>
      </Link>)}</div>
      <nav className={styles.libraryPagination} aria-label="게임 목록 페이지"><button disabled={page === 0} onClick={() => setPage(value => value - 1)}>이전</button><span>{page + 1} / {result.totalPages}</span><button disabled={page + 1 >= result.totalPages} onClick={() => setPage(value => value + 1)}>다음</button></nav>
    </>}
  </section>;
}

function LibraryCover({ game }: { game: LibraryGame }) {
  const [broken, setBroken] = useState(false);
  const url = coverUrl(game.coverImageUrl);
  // eslint-disable-next-line @next/next/no-img-element
  return url && !broken ? <img className={styles.libraryCover} src={url} alt={game.title} loading="lazy" onError={() => setBroken(true)} /> : <div className={styles.libraryCoverFallback}>🎮</div>;
}
