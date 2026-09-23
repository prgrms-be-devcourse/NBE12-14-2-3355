"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";
import AuthNav from "@/components/auth/auth-nav";
import GameSearch from "@/features/games/components/game-search";
import type { Game } from "@/features/games/model";
import Image from "next/image";

export default function SiteHeader() {
  const pathname = usePathname();
  const router = useRouter();
  const [search, setSearch] = useState("");

  function searchGames() {
    const keyword = search.trim();
    router.push(keyword ? `/games?keyword=${encodeURIComponent(keyword)}` : "/games");
  }

  function openGame(game: Game) {
    router.push(`/games/${game.id}`);
  }

  return <header className="header">
    <div className="header-inner">
      <Link className="logo" href="/" aria-label="GameLog 홈">
        <Image
          src="/gamelog-logo-console.png"
          alt=""
          width={28}
          height={28}
        />
        GameLog<span className="lime">.</span>
      </Link>
      <nav aria-label="주요 메뉴">
        <Link className={pathname === "/games" ? "nav-active" : undefined} href="/games" aria-current={pathname === "/games" ? "page" : undefined}>
          전체 게임
        </Link>
      </nav>
      <GameSearch value={search} demo={false} onChange={setSearch} onSearch={searchGames} onSelect={openGame} />
      <AuthNav />
      <span className="header-note">PLAY. RECORD. DISCOVER.</span>
    </div>
  </header>;
}
