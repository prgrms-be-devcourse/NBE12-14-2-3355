"use client";

import { useId, useState } from "react";
import styles from "./select-dropdown.module.css";

type Option<T extends string> = { value: T; label: string };

type Props<T extends string> = {
  value: T;
  options: Option<T>[];
  onChange: (value: T) => void;
  ariaLabel: string;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
};

// 정렬/필터 등 단순 선택 드롭다운에서 재사용하는 컴포넌트. 검색 자동완성(search-dropdown)과 같은 패널 디자인을 공유한다.
export default function SelectDropdown<T extends string>({
  value, options, onChange, ariaLabel, placeholder = "선택", disabled = false, className,
}: Props<T>) {
  const listId = useId();
  const [open, setOpen] = useState(false);
  const selected = options.find(option => option.value === value);
  const label = selected ? selected.label : placeholder;

  function select(next: T) {
    onChange(next);
    setOpen(false);
  }

  return <div className={className ? `${styles.wrap} ${className}` : styles.wrap}
    onBlur={event => { if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false); }}>
    <button type="button" className={styles.trigger} aria-label={ariaLabel} aria-haspopup="listbox" aria-expanded={open}
      aria-controls={open ? listId : undefined} disabled={disabled} onClick={() => setOpen(current => !current)}>
      <span>{label}</span>
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="m6 9 6 6 6-6" /></svg>
    </button>
    {open && <div className="search-dropdown">
      <ul id={listId} role="listbox" aria-label={ariaLabel}>
        {options.map(option => <li key={option.value} role="option" aria-selected={option.value === value}
          onPointerDown={event => event.preventDefault()} onClick={() => select(option.value)}>{option.label}</li>)}
      </ul>
    </div>}
  </div>;
}
