"use client";

import { useEffect, useId, useRef, useState } from "react";
import { coverUrl, demoGames, type Game } from "@/lib/games";
import { acceptRefreshedToken } from "@/features/auth/api";

type Props = {
  value: string;
  demo: boolean;
  onChange: (value: string) => void;
  onSearch: () => void;
  onSelect: (game: Game) => void;
  libraryAccessToken?: string;
};

export default function GameSearch({ value, demo, onChange, onSearch, onSelect, libraryAccessToken }: Props) {
  const listId = useId();
  const composing = useRef(false);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const [result, setResult] = useState<{ key: string; games: Game[]; error: string } | null>(null);
  const keyword = value.trim();
  const requestKey = JSON.stringify([keyword, demo, libraryAccessToken]);
  const visible = open && keyword.length > 0;
  const current = result?.key === requestKey ? result : null;
  const games = current?.games ?? [];

  useEffect(() => {
    if (!visible) return;
    const controller = new AbortController();
    const timer = setTimeout(async () => {
      try {
        let matches: Game[];
        if (libraryAccessToken) {
          const query = new URLSearchParams({ keyword, status: "ALL", sort: "TITLE", page: "0", size: "6" });
          const response = await fetch(`/api/library/games?${query}`, {
            headers: { Authorization: `Bearer ${libraryAccessToken}` }, signal: controller.signal, cache: "no-store",
          });
          acceptRefreshedToken(response);
          if (!response.ok) throw new Error("검색 실패");
          const body = await response.json();
          matches = body.data.userGames.map((game: { gameId: number; title: string; coverImageUrl: string | null }) => ({
            id: game.gameId, title: game.title, coverImageUrl: game.coverImageUrl, releaseDate: null, igdbRating: null,
          }));
        } else if (demo) {
          const term = keyword.toLowerCase();
          const rank = (title: string) => title.toLowerCase() === term ? 0 : title.toLowerCase().startsWith(term) ? 1 : 2;
          matches = demoGames.filter(game => game.title.toLowerCase().includes(term))
            .sort((a, b) => rank(a.title) - rank(b.title) || a.title.toLowerCase().localeCompare(b.title.toLowerCase()) || a.id - b.id).slice(0, 6);
        } else {
          const query = new URLSearchParams({ keyword });
          const response = await fetch(`/api/games/suggestions?${query}`, { signal: controller.signal });
          if (!response.ok) throw new Error("검색 실패");
          const body = await response.json();
          matches = body.data;
        }
        if (!controller.signal.aborted) setResult({ key: requestKey, games: matches, error: "" });
      } catch {
        if (!controller.signal.aborted) setResult({ key: requestKey, games: [], error: "검색 후보를 불러오지 못했어요. 엔터로 다시 검색해 주세요." });
      }
    }, 300);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [keyword, demo, requestKey, visible, libraryAccessToken]);

  function select(game: Game) { setOpen(false); setActive(-1); onSelect(game); }

  return <form className="header-search" onBlur={event => {
    if (!event.currentTarget.contains(event.relatedTarget)) { setOpen(false); setActive(-1); }
  }} onSubmit={event => {
    event.preventDefault();
    if (composing.current) return;
    setOpen(false); setActive(-1); onSearch();
  }}>
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></svg>
    <input role="combobox" aria-label={libraryAccessToken ? "라이브러리 게임 검색" : "게임 제목 검색"} aria-autocomplete="list" aria-expanded={visible}
      aria-controls={visible ? listId : undefined} aria-activedescendant={visible && games[active] ? `${listId}-${games[active].id}` : undefined}
      autoComplete="off" placeholder={libraryAccessToken ? "라이브러리 내 게임 검색" : "다음 게임을 찾아보세요"} maxLength={255} value={value}
      onCompositionStart={() => { composing.current = true; }} onCompositionEnd={() => { composing.current = false; }}
      onFocus={() => setOpen(true)} onChange={event => { onChange(event.target.value); setOpen(true); setActive(-1); setResult(null); }}
      onKeyDown={event => {
        if (event.nativeEvent.isComposing || composing.current || event.keyCode === 229) return;
        if (event.key === "Escape") { event.preventDefault(); setOpen(false); setActive(-1); }
        if (event.key === "ArrowDown" || event.key === "ArrowUp") {
          event.preventDefault(); setOpen(true);
          if (games.length) setActive(index => event.key === "ArrowDown" ? (index + 1) % games.length : (index <= 0 ? games.length - 1 : index - 1));
        }
        if (event.key === "Enter" && visible && games[active]) { event.preventDefault(); select(games[active]); }
      }}/>
    <button type="submit">검색 <span>↵</span></button>
    {visible && <div className="search-dropdown">
      <ul id={listId} role="listbox" aria-label="게임 검색 후보" aria-busy={!current}>
        {games.map((game, index) => <li key={game.id} id={`${listId}-${game.id}`} role="option" aria-selected={index === active}
          onPointerDown={event => event.preventDefault()} onClick={() => select(game)} onPointerMove={() => setActive(index)}>
          <SearchCover game={game}/><div><strong>{game.title}</strong><small>{libraryAccessToken ? "내 라이브러리" : game.releaseDate?.slice(0, 4) || "출시일 미정"}</small></div>
        </li>)}
      </ul>
      <p className="search-status" role="status">{!current ? "검색 중…" : current.error || (!games.length ? "검색 결과가 없어요." : "↑ ↓ 선택 · Enter 검색 또는 선택 · Esc 닫기")}</p>
    </div>}
  </form>;
}

function SearchCover({ game }: { game: Game }) {
  const [broken, setBroken] = useState(false);
  const url = coverUrl(game.coverImageUrl);
  // eslint-disable-next-line @next/next/no-img-element
  return url && !broken ? <img src={url} alt="" onError={() => setBroken(true)}/> : <span className="search-cover-fallback" aria-hidden="true">🎮</span>;
}
