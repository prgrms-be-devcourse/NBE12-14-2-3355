"use client";

import { searchUsers } from "@/features/follows/api";
import type { FollowUserResponseDto } from "@/features/follows/types";
import SearchCombobox from "@/components/ui/search-combobox";
import ImageWithFallback from "@/components/ui/image-with-fallback";
import styles from "./follow-list.module.css";

type Props = {
  value: string;
  accessToken: string;
  onChange: (value: string) => void;
  onSearch: () => void;
  onSelect: (user: FollowUserResponseDto) => void;
};

export default function UserSearch({ value, accessToken, onChange, onSearch, onSelect }: Props) {
  async function fetchResults(keyword: string, signal: AbortSignal): Promise<FollowUserResponseDto[]> {
    const response = await searchUsers(keyword, 0, accessToken, signal, 6);
    return response.users;
  }

  return <SearchCombobox<FollowUserResponseDto>
    value={value} onChange={onChange} onSearch={onSearch} onSelect={onSelect}
    fetchResults={fetchResults} getKey={user => user.userId} maxLength={50} className={styles.userSearch}
    ariaLabel="유저 닉네임 검색" placeholder="유저 닉네임을 검색해 보세요"
    renderItem={user => <><UserAvatar user={user} /><div><strong>{user.nickname}</strong><small>{user.followedByMe ? "Following" : "유저"}</small></div></>}
  />;
}

function UserAvatar({ user }: { user: FollowUserResponseDto }) {
  return <ImageWithFallback
    src={user.profileImageUrl} alt=""
    fallback={<span className="search-cover-fallback" aria-hidden="true">{user.nickname.slice(0, 1).toUpperCase()}</span>}
  />;
}
