"use client";

import { useEffect, useId, useRef, useState, type ReactNode } from "react";

type Props<T> = {
  value: string;
  onChange: (value: string) => void;
  onSearch: () => void;
  onSelect: (item: T) => void;
  fetchResults: (keyword: string, signal: AbortSignal) => Promise<T[]>;
  getKey: (item: T) => string | number;
  renderItem: (item: T) => ReactNode;
  ariaLabel: string;
  placeholder: string;
  emptyText?: string;
  errorText?: string;
  maxLength?: number;
  debounceMs?: number;
  className?: string;
};

// 게임/유저 검색 등 "입력 -> 자동완성 드롭다운" 화면 전체에서 재사용하는 검색 콤보박스
export default function SearchCombobox<T>({
  value, onChange, onSearch, onSelect, fetchResults, getKey, renderItem, ariaLabel, placeholder,
  emptyText = "검색 결과가 없어요.", errorText = "검색 후보를 불러오지 못했어요. 엔터로 다시 검색해 주세요.",
  maxLength = 255, debounceMs = 300, className,
}: Props<T>) {
  const listId = useId();
  const composing = useRef(false);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const [result, setResult] = useState<{ key: string; items: T[]; error: string } | null>(null);
  const keyword = value.trim();
  const requestKey = keyword;
  const visible = open && keyword.length > 0;
  const current = result?.key === requestKey ? result : null;
  const items = current?.items ?? [];

  useEffect(() => {
    if (!visible) return;
    const controller = new AbortController();
    const timer = setTimeout(async () => {
      try {
        const matches = await fetchResults(keyword, controller.signal);
        if (!controller.signal.aborted) setResult({ key: requestKey, items: matches, error: "" });
      } catch {
        if (!controller.signal.aborted) setResult({ key: requestKey, items: [], error: errorText });
      }
    }, debounceMs);
    return () => { clearTimeout(timer); controller.abort(); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [keyword, requestKey, visible, debounceMs, errorText]);

  function select(item: T) { setOpen(false); setActive(-1); onSelect(item); }

  return <form className={className ? `header-search ${className}` : "header-search"} onBlur={event => {
    if (!event.currentTarget.contains(event.relatedTarget)) { setOpen(false); setActive(-1); }
  }} onSubmit={event => {
    event.preventDefault();
    if (composing.current) return;
    setOpen(false); setActive(-1); onSearch();
  }}>
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 5 5" /></svg>
    <input role="combobox" aria-label={ariaLabel} aria-autocomplete="list" aria-expanded={visible}
      aria-controls={visible ? listId : undefined} aria-activedescendant={visible && items[active] ? `${listId}-${getKey(items[active])}` : undefined}
      autoComplete="off" placeholder={placeholder} maxLength={maxLength} value={value}
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
          if (items.length) setActive(index => event.key === "ArrowDown" ? (index + 1) % items.length : (index <= 0 ? items.length - 1 : index - 1));
        }
        if (event.key === "Enter" && visible && items[active]) { event.preventDefault(); select(items[active]); }
      }} />
    <button type="submit">검색 <span>↵</span></button>
    {visible && <div className="search-dropdown">
      <ul id={listId} role="listbox" aria-label={ariaLabel} aria-busy={!current}>
        {!current
          ? Array.from({ length: 4 }, (_, index) => <li key={index} className="search-skeleton-row" aria-hidden="true">
            <span className="search-skeleton-cover" /><span className="search-skeleton-text" />
          </li>)
          : items.map((item, index) => <li key={getKey(item)} id={`${listId}-${getKey(item)}`} role="option" aria-selected={index === active}
            onPointerDown={event => event.preventDefault()} onClick={() => select(item)} onPointerMove={() => setActive(index)}>
            {renderItem(item)}
          </li>)}
      </ul>
      {current && <p className="search-status" role="status">{current.error || (!items.length ? emptyText : "↑ ↓ 선택 · Enter 검색 또는 선택 · Esc 닫기")}</p>}
    </div>}
  </form>;
}
