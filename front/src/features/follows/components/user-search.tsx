"use client";

import { useEffect, useId, useRef, useState } from "react";
import { searchUsers } from "@/features/follows/api";
import type { FollowUserResponseDto } from "@/features/follows/types";
import styles from "./follow-list.module.css";

type Props = {
  value: string;
  accessToken: string;
  onChange: (value: string) => void;
  onSearch: () => void;
  onSelect: (user: FollowUserResponseDto) => void;
};

export default function UserSearch({ value, accessToken, onChange, onSearch, onSelect }: Props) {
  const listId = useId();
  const composing = useRef(false);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const [result, setResult] = useState<{ key: string; users: FollowUserResponseDto[]; error: string } | null>(null);
  const keyword = value.trim();
  const requestKey = JSON.stringify([keyword, accessToken]);
  const visible = open && keyword.length > 0;
  const current = result?.key === requestKey ? result : null;
  const users = current?.users ?? [];

  useEffect(() => {
    if (!visible) return;
    const controller = new AbortController();
    const timer = setTimeout(async () => {
      try {
        const response = await searchUsers(keyword, 0, accessToken, controller.signal, 6);
        if (!controller.signal.aborted) setResult({ key: requestKey, users: response.users, error: "" });
      } catch {
        if (!controller.signal.aborted) setResult({ key: requestKey, users: [], error: "검색 후보를 불러오지 못했어요. 엔터로 다시 검색해 주세요." });
      }
    }, 300);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [keyword, accessToken, requestKey, visible]);

  function select(user: FollowUserResponseDto) { setOpen(false); setActive(-1); onSelect(user); }

  return <form className={`header-search ${styles.userSearch}`} onBlur={event => {
    if (!event.currentTarget.contains(event.relatedTarget)) { setOpen(false); setActive(-1); }
  }} onSubmit={event => {
    event.preventDefault();
    if (composing.current) return;
    setOpen(false); setActive(-1); onSearch();
  }}>
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></svg>
    <input role="combobox" aria-label="유저 닉네임 검색" aria-autocomplete="list" aria-expanded={visible}
      aria-controls={visible ? listId : undefined} aria-activedescendant={visible && users[active] ? `${listId}-${users[active].userId}` : undefined}
      autoComplete="off" placeholder="유저 닉네임을 검색해 보세요" maxLength={50} value={value}
      onCompositionStart={() => { composing.current = true; }} onCompositionEnd={() => { composing.current = false; }}
      onFocus={() => setOpen(true)} onChange={event => { onChange(event.target.value); setOpen(true); setActive(-1); setResult(null); }}
      onKeyDown={event => {
        if (event.nativeEvent.isComposing || composing.current || event.keyCode === 229) {
          if (event.key === "Enter") event.preventDefault();
          return;
        }
        if (event.key === "Escape") { event.preventDefault(); setOpen(false); setActive(-1); }
        if (event.key === "ArrowDown" || event.key === "ArrowUp") {
          event.preventDefault(); setOpen(true);
          if (users.length) setActive(index => event.key === "ArrowDown" ? (index + 1) % users.length : (index <= 0 ? users.length - 1 : index - 1));
        }
        if (event.key === "Enter" && visible && users[active]) { event.preventDefault(); select(users[active]); }
      }} />
    <button type="submit">검색 <span>↵</span></button>
    {visible && <div className="search-dropdown">
      <ul id={listId} role="listbox" aria-label="유저 검색 후보" aria-busy={!current}>
        {users.map((user, index) => <li key={user.userId} id={`${listId}-${user.userId}`} role="option" aria-selected={index === active}
          onPointerDown={event => event.preventDefault()} onClick={() => select(user)} onPointerMove={() => setActive(index)}>
          <UserAvatar key={`${user.userId}/${user.profileImageUrl}`} user={user} />
          <div><strong>{user.nickname}</strong><small>{user.followedByMe ? "Following" : "유저"}</small></div>
        </li>)}
      </ul>
      <p className="search-status" role="status">{!current ? "검색 중…" : current.error || (!users.length ? "검색 결과가 없어요." : "↑ ↓ 선택 · Enter 검색 또는 선택 · Esc 닫기")}</p>
    </div>}
  </form>;
}

function UserAvatar({ user }: { user: FollowUserResponseDto }) {
  const [broken, setBroken] = useState(false);
  return user.profileImageUrl && !broken
    // eslint-disable-next-line @next/next/no-img-element
    ? <img src={user.profileImageUrl} alt="" onError={() => setBroken(true)} />
    : <span className="search-cover-fallback" aria-hidden="true">{user.nickname.slice(0, 1).toUpperCase()}</span>;
}
