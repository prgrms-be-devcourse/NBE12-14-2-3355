"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/features/auth/auth-context";
import { completeOnboarding, setPreferredGames, setPreferredGenres, skipOnboarding } from "@/features/auth/api";
import { isOnboarded } from "@/features/auth/onboarding-status";
import { coverUrl, type Game, type Option } from "@/lib/games";
import styles from "./onboarding-client.module.css";

const MAX_GENRES = 3;
const MAX_GAMES = 3;

export default function OnboardingClient() {
  const router = useRouter();
  const auth = useAuth();
  const [step, setStep] = useState<1 | 2>(1);
  const [genreOptions, setGenreOptions] = useState<Option[]>([]);
  const [selectedGenreIds, setSelectedGenreIds] = useState<number[]>([]);
  const [shakeGenreId, setShakeGenreId] = useState<number | null>(null);
  const [shakeGameId, setShakeGameId] = useState<number | null>(null);
  const [curatedResult, setCuratedResult] = useState<{ key: string; games: Game[]; page: number; totalPages: number } | null>(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const [gameQuery, setGameQuery] = useState("");
  const [gameResult, setGameResult] = useState<{ key: string; games: Game[] } | null>(null);
  const [selectedGames, setSelectedGames] = useState<Game[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (auth.status === "unauthenticated") router.replace("/login?next=/onboarding");
  }, [auth.status, router]);

  useEffect(() => {
    if (auth.user && isOnboarded(auth.user)) router.replace("/");
  }, [auth.user, router]);

  useEffect(() => {
    if (auth.status !== "authenticated") return;
    const controller = new AbortController();
    fetch("/api/games/filters", { signal: controller.signal })
      .then(async (response) => {
        const body = await response.json();
        if (!response.ok) throw new Error();
        setGenreOptions(body.data.genres ?? []);
      })
      .catch(() => {});
    return () => controller.abort();
  }, [auth.status]);

  useEffect(() => {
    if (shakeGenreId === null) return;
    const timer = setTimeout(() => setShakeGenreId(null), 400);
    return () => clearTimeout(timer);
  }, [shakeGenreId]);

  useEffect(() => {
    if (shakeGameId === null) return;
    const timer = setTimeout(() => setShakeGameId(null), 400);
    return () => clearTimeout(timer);
  }, [shakeGameId]);

  const genreKey = selectedGenreIds.slice().sort((a, b) => a - b).join(",");
  const curatedKey = `${step}:${genreKey}`;
  useEffect(() => {
    if (step !== 2) return;
    const controller = new AbortController();
    const query = new URLSearchParams({ sort: "RATING", size: "12", page: "0" });
    selectedGenreIds.forEach((id) => query.append("genreIds", String(id)));
    fetch(`/api/games/page?${query}`, { signal: controller.signal })
      .then(async (response) => {
        const body = await response.json();
        if (!response.ok) throw new Error();
        if (!controller.signal.aborted) {
          setCuratedResult({ key: curatedKey, games: body.data.content ?? [], page: 0, totalPages: body.data.totalPages ?? 0 });
        }
      })
      .catch(() => { if (!controller.signal.aborted) setCuratedResult({ key: curatedKey, games: [], page: 0, totalPages: 0 }); });
    return () => controller.abort();
  }, [step, selectedGenreIds, curatedKey]);
  const curatedGames = curatedResult?.key === curatedKey ? curatedResult.games : [];
  const curatedLoading = step === 2 && curatedResult?.key !== curatedKey;
  const hasMoreGames = curatedResult?.key === curatedKey && curatedResult.page + 1 < curatedResult.totalPages;

  async function loadMoreGames() {
    if (!curatedResult || curatedResult.key !== curatedKey) return;
    const nextPage = curatedResult.page + 1;
    setLoadingMore(true);
    try {
      const query = new URLSearchParams({ sort: "RATING", size: "12", page: String(nextPage) });
      selectedGenreIds.forEach((id) => query.append("genreIds", String(id)));
      const response = await fetch(`/api/games/page?${query}`);
      const body = await response.json();
      if (!response.ok) throw new Error();
      setCuratedResult((current) => current && current.key === curatedKey
        ? { key: curatedKey, games: [...current.games, ...(body.data.content ?? [])], page: nextPage, totalPages: body.data.totalPages ?? current.totalPages }
        : current);
    } catch {
      // 실패해도 기존 목록은 유지, 버튼을 다시 누르면 재시도됨
    } finally {
      setLoadingMore(false);
    }
  }

  const keyword = gameQuery.trim();
  useEffect(() => {
    if (!keyword) return;
    const controller = new AbortController();
    const timer = setTimeout(async () => {
      try {
        const query = new URLSearchParams({ keyword });
        const response = await fetch(`/api/games/suggestions?${query}`, { signal: controller.signal });
        if (!response.ok) throw new Error();
        const body = await response.json();
        if (!controller.signal.aborted) setGameResult({ key: keyword, games: body.data ?? [] });
      } catch {
        if (!controller.signal.aborted) setGameResult({ key: keyword, games: [] });
      }
    }, 300);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [keyword]);
  const gameResults = keyword && gameResult?.key === keyword ? gameResult.games : [];

  function toggleGenre(id: number) {
    setSelectedGenreIds((current) => {
      if (current.includes(id)) return current.filter((x) => x !== id);
      if (current.length >= MAX_GENRES) {
        setShakeGenreId(id);
        return current;
      }
      return [...current, id];
    });
  }

  function toggleGame(game: Game) {
    setSelectedGames((current) => {
      if (current.some((g) => g.id === game.id)) return current.filter((g) => g.id !== game.id);
      if (current.length >= MAX_GAMES) {
        setShakeGameId(game.id);
        return current;
      }
      return [...current, game];
    });
  }

  function removeGame(id: number) {
    setSelectedGames((current) => current.filter((g) => g.id !== id));
  }

  async function complete() {
    if (!auth.accessToken) return;
    setSaving(true);
    setError("");
    try {
      if (selectedGenreIds.length) await setPreferredGenres(selectedGenreIds, auth.accessToken);
      if (selectedGames.length) await setPreferredGames(selectedGames.map((g) => g.id), auth.accessToken);
      const updated = await completeOnboarding(auth.accessToken);
      auth.setUser(updated);
      router.replace("/");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "온보딩 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  }

  async function skip() {
    if (!auth.accessToken) return;
    setSaving(true);
    setError("");
    try {
      const updated = await skipOnboarding(auth.accessToken);
      auth.setUser(updated);
      router.replace("/");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "건너뛰기에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  }

  if (auth.status !== "authenticated") {
    return (
      <div className={styles.shell}>
        <Link href="/" className={styles.logo} aria-label="GameLog 홈">GameLog<span className="lime">.</span></Link>
        <p className={styles.loading}>{auth.status === "loading" ? "확인 중…" : "로그인 페이지로 이동 중…"}</p>
      </div>
    );
  }

  return (
    <div className={styles.shell}>
      <Link href="/" className={styles.logo} aria-label="GameLog 홈">GameLog<span className="lime">.</span></Link>

      <div className={styles.panel}>
        <div className={styles.stepIndicator}>
          <span className={step === 1 ? styles.stepActive : styles.stepDone}>1. 선호 장르</span>
          <span className={styles.stepDivider} />
          <span className={step === 2 ? styles.stepActive : styles.stepPending}>2. 선호 게임</span>
        </div>

        {error && <p className={styles.error} role="alert">{error}</p>}

        {step === 1 && (
          <section className={styles.step}>
            <span className={styles.eyebrow}>GET STARTED</span>
            <h1 className={styles.title}>좋아하는 장르를 선택해 주세요</h1>
            <p className={styles.subtitle}>최대 {MAX_GENRES}개까지 고를 수 있어요 ({selectedGenreIds.length}/{MAX_GENRES})</p>
            <div className={styles.genreGrid}>
              {genreOptions.map((genre) => {
                const selected = selectedGenreIds.includes(genre.id);
                const base = selected ? styles.genreSelected : styles.genre;
                const className = shakeGenreId === genre.id ? `${base} ${styles.genreShake}` : base;
                return (
                  <button key={genre.id} type="button" className={className} onClick={() => toggleGenre(genre.id)}>
                    {genre.name}
                  </button>
                );
              })}
            </div>
            <div className={styles.actions}>
              <button type="button" className={styles.skip} onClick={skip} disabled={saving}>나중에 하기</button>
              <button type="button" className={styles.complete} onClick={() => setStep(2)} disabled={saving}>다음</button>
            </div>
          </section>
        )}

        {step === 2 && (
          <section className={styles.step}>
            <span className={styles.eyebrow}>ALMOST DONE</span>
            <h1 className={styles.title}>좋아하는 게임을 선택해 주세요</h1>
            <p className={styles.subtitle}>최대 {MAX_GAMES}개까지 고를 수 있어요 ({selectedGames.length}/{MAX_GAMES})</p>

            <input
              className={styles.searchInput}
              value={gameQuery}
              onChange={(event) => setGameQuery(event.target.value)}
              placeholder="게임 제목을 검색해 보세요"
            />
            {selectedGames.length > 0 && (
              <div className={styles.chips}>
                {selectedGames.map((game) => (
                  <button key={game.id} type="button" onClick={() => removeGame(game.id)}>{game.title} ×</button>
                ))}
              </div>
            )}
            {gameResults.length > 0 && (
              <ul className={styles.resultList}>
                {gameResults.map((game) => {
                  const cover = coverUrl(game.coverImageUrl);
                  const picked = selectedGames.some((g) => g.id === game.id);
                  return (
                    <li key={game.id}>
                      <button
                        type="button"
                        className={shakeGameId === game.id ? styles.resultShake : undefined}
                        onClick={() => toggleGame(game)}
                        disabled={picked}
                      >
                        {cover
                          ? <img className={styles.resultCover} src={cover} alt="" />
                          : <span className={styles.resultCoverFallback}>{game.title.slice(0, 1)}</span>}
                        <span>{picked ? `${game.title} (선택됨)` : game.title}</span>
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}

            {curatedLoading && <p className={styles.hint}>게임을 불러오는 중…</p>}
            {!curatedLoading && curatedGames.length === 0 && <p className={styles.hint}>추천할 게임을 찾지 못했어요.</p>}
            <div className={styles.gameGrid}>
              {curatedGames.map((game) => {
                const cover = coverUrl(game.coverImageUrl);
                const picked = selectedGames.some((g) => g.id === game.id);
                const base = picked ? styles.gameCardSelected : styles.gameCard;
                const className = shakeGameId === game.id ? `${base} ${styles.gameCardShake}` : base;
                return (
                  <button key={game.id} type="button" className={className} onClick={() => toggleGame(game)}>
                    {cover
                      ? <img className={styles.gameCover} src={cover} alt="" />
                      : <span className={styles.gameCoverFallback}>{game.title.slice(0, 1)}</span>}
                    <span className={styles.gameCardTitle}>{game.title}</span>
                  </button>
                );
              })}
            </div>
            {hasMoreGames && (
              <button type="button" className={styles.loadMore} onClick={loadMoreGames} disabled={loadingMore}>
                {loadingMore ? "불러오는 중…" : "더보기"}
              </button>
            )}

            <div className={styles.actions}>
              <button type="button" className={styles.back} onClick={() => setStep(1)} disabled={saving}>이전</button>
              <div className={styles.actionsRight}>
                <button type="button" className={styles.skip} onClick={skip} disabled={saving}>나중에 하기</button>
                <button type="button" className={styles.complete} onClick={complete} disabled={saving}>{saving ? "저장 중…" : "완료"}</button>
              </div>
            </div>
          </section>
        )}
      </div>
    </div>
  );
}
