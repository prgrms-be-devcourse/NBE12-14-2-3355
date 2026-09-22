"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import GameSearch from "@/components/game-search";
import AuthNav from "@/components/auth/auth-nav";
import PersonalizedGames from "@/components/personalized-games";
import { coverUrl, demoGames, demoOptions, emptyFilters, type Filters, type Game, type GamePage, type Option } from "@/lib/games";

function Icon({ name, size = 20 }: { name: "search" | "game" | "filter" | "arrow" | "close"; size?: number }) {
  const paths = {
    search: <><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></>,
    game: <><path d="M7 6h10c3 0 5 10 3 12-2 2-4-2-5-2H9c-1 0-3 4-5 2C2 16 4 6 7 6Z"/><path d="M6 11h5M8.5 8.5v5M16 10h.01M18 13h.01"/></>,
    filter: <><path d="M3 6h18M3 12h18M3 18h18"/><path d="M8 3v6M16 9v6M9 15v6"/></>,
    arrow: <path d="M5 12h14m-6-6 6 6-6 6"/>,
    close: <path d="m6 6 12 12M6 18 18 6"/>,
  };
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>;
}

function Cover({ game }: { game: Game }) {
  const [broken, setBroken] = useState(false);
  const url = coverUrl(game.coverImageUrl);
  return url && !broken
    // Covers are served by IGDB or the preview CDN, with an explicit missing-image fallback.
    // eslint-disable-next-line @next/next/no-img-element
    ? <img src={url} alt={`${game.title} 커버`} loading="lazy" onError={() => setBroken(true)}/>
    : <div className="cover-fallback"><Icon name="game" size={36}/><span>{game.title}</span><small>커버 준비 중</small></div>;
}

function Detail({ game, onClose }: { game: Game; onClose: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    dialog.current?.showModal();
  }, []);
  return <dialog ref={dialog} className="detail" onCancel={onClose} onClick={e => { if (e.target === e.currentTarget) onClose(); }} aria-labelledby="detail-title">
    <button className="icon-button close-detail" onClick={onClose} aria-label="상세 정보 닫기"><Icon name="close"/></button>
    <div className="detail-cover"><Cover game={game}/></div>
    <div className="detail-copy"><span className="eyebrow">GAME DETAILS</span><h2 id="detail-title">{game.title}</h2>
      <p>{game.releaseDate || "출시일 미정"}{game.developer ? ` · ${game.developer}` : ""}</p>
      <div className="chips">{game.genres?.map(g => <span key={g.id}>{g.name}</span>)}</div>
      {game.platforms?.length ? <p className="platforms">{game.platforms.map(p => p.name).join(" · ")}</p> : null}
      <p className="description">{game.description || "등록된 소개가 없습니다."}</p>
      {game.igdbRating != null && <p>IGDB 평점 <strong>{game.igdbRating.toFixed(1)}</strong> / 100</p>}
    </div>
  </dialog>;
}

export default function Discover() {
  const router = useRouter();
  const [demo, setDemo] = useState(false);
  const [options, setOptions] = useState<{ genres: Option[]; platforms: Option[] }>({ genres: [], platforms: [] });
  const [optionsError, setOptionsError] = useState("");
  const [input, setInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [draft, setDraft] = useState<Filters>(emptyFilters);
  const [filters, setFilters] = useState<Filters>(emptyFilters);
  const [sort, setSort] = useState("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<GamePage | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [retry, setRetry] = useState(0);
  const [mobileFilters, setMobileFilters] = useState(false);
  const [selected, setSelected] = useState<Game | null>(null);
  const filterCount = filters.genres.length + filters.platforms.length;
  const size = 12;

  useEffect(() => {
    const controller = new AbortController();
    if (demo) return;
    fetch("/api/games/filters", { signal: controller.signal }).then(async r => {
      const body = await r.json();
      if (!r.ok) throw new Error();
      setOptions(body.data);
      setOptionsError("");
    }).catch(e => { if (e.name !== "AbortError") setOptionsError("필터 목록을 불러오지 못했어요."); });
    return () => controller.abort();
  }, [demo, retry]);

  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      setLoading(true);
      setError("");
      try {
        if (demo) {
          const games = demoGames.filter(g => g.title.toLowerCase().includes(keyword.toLowerCase())
            && (!filters.genres.length || g.genres?.some(x => filters.genres.includes(x.id)))
            && (!filters.platforms.length || g.platforms?.some(x => filters.platforms.includes(x.id))));
          games.sort((a, b) => (sort === "TITLE" ? a.title.localeCompare(b.title) : sort === "LATEST" ? (b.releaseDate || "").localeCompare(a.releaseDate || "") : 0) || a.id - b.id);
          setResult({ content: games.slice(page * size, (page + 1) * size), totalElements: games.length, totalPages: Math.ceil(games.length / size), number: page });
        } else {
          const query = new URLSearchParams({ page: String(page), size: String(size) });
          if (keyword) query.set("keyword", keyword);
          if (sort) query.set("sort", sort);
          filters.genres.forEach(id => query.append("genreIds", String(id)));
          filters.platforms.forEach(id => query.append("platformIds", String(id)));
          const response = await fetch(`/api/games/page?${query}`, { signal: controller.signal });
          const body = await response.json();
          if (!response.ok) throw new Error(body.msg || "게임을 불러오지 못했어요.");
          setResult(body.data);
        }
      } catch (e) {
        if (e instanceof Error && e.name !== "AbortError") { setError(e.message); setResult(null); }
      } finally { if (!controller.signal.aborted) setLoading(false); }
    }
    void load();
    return () => controller.abort();
  }, [keyword, filters, sort, page, demo, retry]);

  const available = demo ? demoOptions : options;
  function submitSearch() { setKeyword(input.trim()); setPage(0); setRetry(x => x + 1); }
  function toggleFilter(type: keyof Filters, id: number) {
    setDraft(current => ({ ...current, [type]: current[type].includes(id) ? current[type].filter(x => x !== id) : [...current[type], id] }));
  }
  function reset() { setDraft(emptyFilters); setFilters(emptyFilters); setKeyword(""); setInput(""); setPage(0); }
  function switchMode() { setDemo(!demo); reset(); setResult(null); setLoading(true); }
  function openGame(game: Game) {
    if (demo) setSelected(game);
    else router.push(`/games/${game.id}`);
  }
  function removeFilter(type: keyof Filters, id: number) {
    const next = { ...filters, [type]: filters[type].filter(x => x !== id) };
    setFilters(next); setDraft(next); setPage(0);
  }
  const totalPages = result?.totalPages || 0;
  const firstPage = Math.max(0, Math.min(page - 2, totalPages - 5));
  const pages = Array.from({ length: Math.min(5, totalPages) }, (_, i) => firstPage + i);

  return <>
    <header className="header"><div className="header-inner">
      <Link className="logo" href="/" aria-label="GameLog 홈"><Icon name="game" size={29}/><span>GameLog<span className="lime">.</span></span></Link>
      <nav aria-label="주요 메뉴"><a className="nav-active" href="#discover" aria-current="page">게임 탐색</a><a href="#catalog">전체 게임</a></nav>
      <GameSearch value={input} demo={demo} onChange={setInput} onSearch={submitSearch} onSelect={openGame}/>
      <AuthNav/>
      <span className="header-note">PLAY. RECORD. DISCOVER.</span>
    </div></header>

    <main id="discover" className="main">
      <section className="intro"><div><span className="eyebrow"><span className="dot"/> YOUR NEXT FAVORITE</span><h1>다음에 빠져들 <span>게임을 발견하세요.</span></h1><p>익숙한 취향부터 새로운 세계까지, 나만의 다음 플레이를 찾아보세요.</p></div><div className="intro-mark" aria-hidden="true"><Icon name="game" size={68}/><span>FIND YOUR<br/>NEXT WORLD ↗</span></div></section>
      {!demo && <PersonalizedGames />}
      <div className="section-line"><span>EXPLORE THE COLLECTION</span><span>한 게임의 기록이, 다음 인생 게임을 찾는다.</span></div>
      <div className="workspace">
        <aside className={`filter-panel ${mobileFilters ? "is-open" : ""}`} id="filters">
          <div className="filter-heading"><h2><Icon name="filter"/>필터</h2><button className="text-button" onClick={reset}>초기화 ↺</button></div>
          <p className="filter-help">어떤 세계를 찾고 있나요?</p>
          {!demo && optionsError && <p className="filter-error" role="status">{optionsError}<button className="text-button" onClick={() => setRetry(x => x + 1)}>다시 불러오기</button></p>}
          {(["genres", "platforms"] as const).map(type => <fieldset key={type}><legend>{type === "genres" ? "장르" : "플랫폼"}<span>여러 개 선택 가능</span></legend><div className="filter-options">
            {available[type].map(option => <label key={option.id} className={draft[type].includes(option.id) ? "checked" : ""}><input type="checkbox" checked={draft[type].includes(option.id)} onChange={() => toggleFilter(type, option.id)}/><span>{option.name}</span></label>)}
            {!available[type].length && <span className="muted">{optionsError ? "선택 항목 없음" : "필터를 불러오는 중…"}</span>}
          </div></fieldset>)}
          <button className="apply-button" onClick={() => { setFilters(draft); setPage(0); setMobileFilters(false); }}>필터 적용하기 <Icon name="arrow" size={18}/></button>
          <div className="filter-foot"><span className="dot"/> 취향에 맞는 발견의 시작</div>
        </aside>

        <section className="catalog" id="catalog" aria-label="게임 목록">
          <div className="catalog-toolbar"><div><h2>{keyword ? `“${keyword}” 검색 결과` : "모든 게임"}<span className="count">{loading ? "…" : (result?.totalElements ?? 0).toLocaleString()}</span></h2><p>{filterCount ? `${filterCount}개의 필터가 적용되었어요` : "마음에 드는 커버에서 새로운 이야기를 시작해 보세요."}</p></div>
            <div className="toolbar-actions"><button className="mobile-filter" aria-expanded={mobileFilters} aria-controls="filters" onClick={() => setMobileFilters(!mobileFilters)}><Icon name="filter" size={16}/>필터</button><label className="sort-label">정렬<select aria-label="게임 정렬" value={sort} onChange={e => { setSort(e.target.value); setPage(0); }}><option value="">기본순</option><option value="LATEST">최신 출시일순</option><option value="TITLE">제목순</option><option value="RATING">GameLog 평점순</option><option value="LIBRARY">라이브러리 등록순</option><option value="PLAY_TIME">평균 플레이 타임순</option></select></label></div>
          </div>
          {(filterCount > 0 || keyword) && <div className="chips active-chips">{keyword && <button onClick={() => { setInput(""); setKeyword(""); setPage(0); }}>검색: {keyword} ×</button>}{(["genres", "platforms"] as const).flatMap(type => filters[type].map(id => <button key={`${type}-${id}`} onClick={() => removeFilter(type, id)}>{available[type].find(o => o.id === id)?.name || id} ×</button>))}</div>}
          {demo && <div className="preview-notice"><span><span className="dot"/> 디자인 미리보기 · 샘플 게임 데이터</span><button onClick={switchMode}>실제 게임 불러오기 ↗</button></div>}
          <div aria-live="polite" aria-busy={loading}>
            {loading ? <div className="game-grid skeleton-grid" aria-label="게임을 불러오는 중">{Array.from({ length: 12 }, (_, i) => <div className="skeleton" key={i}/>)}</div> : error ? <div className="empty-state"><Icon name="game" size={44}/><h3>잠시 연결이 끊겼어요</h3><p>{error}</p><div><button className="apply-button" onClick={() => setRetry(x => x + 1)}>다시 시도</button><button className="outline-button" onClick={switchMode}>디자인 미리보기</button></div></div> : !result?.content.length ? <div className="empty-state"><Icon name="search" size={44}/><h3>검색 결과가 없어요</h3><p>다른 검색어를 입력하거나 필터를 조금 줄여보세요.</p><button className="outline-button" onClick={reset}>검색 조건 초기화</button></div> : <div className="game-grid">{result.content.map(game => <article className="game-card" key={game.id}>
              <button className="cover-button" onClick={() => openGame(game)} aria-label={`${game.title} 상세 보기`}><Cover game={game}/><span className="cover-overlay"><span>게임 살펴보기</span><Icon name="arrow"/></span></button>
              <div className="card-caption"><button onClick={() => openGame(game)}>{game.title}</button><span>{game.releaseDate?.slice(0, 4) || "출시일 미정"}</span></div>
              {game.platforms?.length ? <p className="card-platforms">{game.platforms.map(p => p.name).join(" · ")}</p> : null}
            </article>)}</div>}
          </div>
          {!loading && !error && totalPages > 0 && <div className="pagination-wrap"><span>{page * size + 1}–{Math.min((page + 1) * size, result!.totalElements)} / {result!.totalElements.toLocaleString()} games</span><nav className="pagination" aria-label="게임 페이지 이동"><button disabled={page === 0} onClick={() => setPage(0)} aria-label="첫 페이지">«</button><button disabled={page === 0} onClick={() => setPage(p => p - 1)} aria-label="이전 페이지">‹</button>{pages.map(p => <button key={p} aria-current={page === p ? "page" : undefined} onClick={() => setPage(p)}>{p + 1}</button>)}<button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)} aria-label="다음 페이지">›</button><button disabled={page >= totalPages - 1} onClick={() => setPage(totalPages - 1)} aria-label="마지막 페이지">»</button></nav></div>}
        </section>
      </div>
      <section className="closing-note"><Icon name="game" size={28}/><div><h2>좋은 게임은, 또 다른 발견의 시작.</h2><p>당신의 다음 이야기가 이곳에서 시작됩니다.</p></div><span>KEEP EXPLORING ↗</span></section>
    </main>
    <footer><Link className="logo" href="/">GameLog<span className="lime">.</span></Link><span>© 2026 GameLog. 한 게임씩, 나만의 이야기.</span><button className="text-button" onClick={switchMode}>{demo ? "실제 데이터로 전환" : "디자인 미리보기"}</button></footer>
    {selected && <Detail game={selected} onClose={() => setSelected(null)}/>}
  </>;
}
