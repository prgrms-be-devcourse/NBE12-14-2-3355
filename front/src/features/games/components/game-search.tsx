"use client";

import { coverUrl, demoGames, type Game } from "@/features/games/model";
import { acceptRefreshedToken } from "@/features/auth/api";
import SearchCombobox from "@/components/ui/search-combobox";
import ImageWithFallback from "@/components/ui/image-with-fallback";

type Props = {
  value: string;
  demo: boolean;
  onChange: (value: string) => void;
  onSearch: () => void;
  onSelect: (game: Game) => void;
  libraryAccessToken?: string;
  libraryUserId?: number;
};

// 전역 검색과 라이브러리 검색에서 재사용하는 게임 자동완성
export default function GameSearch({ value, demo, onChange, onSearch, onSelect, libraryAccessToken, libraryUserId }: Props) {
  async function fetchResults(keyword: string, signal: AbortSignal): Promise<Game[]> {
    if (libraryAccessToken || libraryUserId !== undefined) {
      const query = new URLSearchParams({ keyword, status: "ALL", sort: "TITLE", page: "0", size: "6" });
      const endpoint = libraryUserId === undefined ? "/api/library/games" : `/api/library/games/profile/${libraryUserId}/games`;
      const response = await fetch(`${endpoint}?${query}`, {
        headers: libraryAccessToken ? { Authorization: `Bearer ${libraryAccessToken}` } : undefined, signal, cache: "no-store",
      });
      acceptRefreshedToken(response);
      if (!response.ok) throw new Error("검색 실패");
      const body = await response.json();
      return body.data.userGames.map((game: { gameId: number; title: string; coverImageUrl: string | null }) => ({
        id: game.gameId, title: game.title, coverImageUrl: game.coverImageUrl, releaseDate: null, igdbRating: null,
      }));
    }
    if (demo) {
      const term = keyword.toLowerCase();
      const rank = (title: string) => title.toLowerCase() === term ? 0 : title.toLowerCase().startsWith(term) ? 1 : 2;
      return demoGames.filter(game => game.title.toLowerCase().includes(term))
        .sort((a, b) => rank(a.title) - rank(b.title) || a.title.toLowerCase().localeCompare(b.title.toLowerCase()) || a.id - b.id).slice(0, 6);
    }
    const query = new URLSearchParams({ keyword });
    const response = await fetch(`/api/games/suggestions?${query}`, { signal });
    if (!response.ok) throw new Error("검색 실패");
    const body = await response.json();
    return body.data;
  }

  return <SearchCombobox<Game>
    value={value} onChange={onChange} onSearch={onSearch} onSelect={onSelect}
    fetchResults={fetchResults} getKey={game => game.id}
    ariaLabel={libraryAccessToken ? "라이브러리 게임 검색" : "게임 제목 검색"}
    placeholder={libraryAccessToken ? "라이브러리 내 게임 검색" : "다음 게임을 찾아보세요"}
    renderItem={game => <><SearchCover game={game} /><div><strong>{game.title}</strong><small>{libraryAccessToken ? "내 라이브러리" : game.releaseDate?.slice(0, 4) || "출시일 미정"}</small></div></>}
  />;
}

function SearchCover({ game }: { game: Game }) {
  return <ImageWithFallback
    src={coverUrl(game.coverImageUrl)} alt=""
    fallback={<span className="search-cover-fallback" aria-hidden="true">🎮</span>}
  />;
}
