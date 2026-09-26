"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useRef, useState } from "react";
import { useAuth } from "@/features/auth/auth-context";
import { FollowApiError, getFollows, searchUsers, setFollowing } from "@/features/follows/api";
import type { FollowKind, FollowPageResponseDto, FollowUserResponseDto } from "@/features/follows/types";
import FollowButton from "./follow-button";
import UserSearch from "./user-search";
import ImageWithFallback from "@/components/ui/image-with-fallback";
import styles from "./follow-list.module.css";

type Props = { userId: number; kind: FollowKind; page: number; basePath: string };

function Avatar({ user }: { user: FollowUserResponseDto }) {
  return <ImageWithFallback
    src={user.profileImageUrl} alt="" className={styles.avatar}
    fallback={<span className={styles.avatar} aria-hidden="true">{user.nickname.slice(0, 1).toUpperCase()}</span>}
  />;
}

export default function FollowList(props: Props) {
  return <Suspense fallback={<p role="status">목록을 불러오는 중…</p>}><FriendsContent {...props} /></Suspense>;
}

function FriendsContent(props: Props) {
  const auth = useAuth();
  const searchParams = useSearchParams();
  const keyword = searchParams.get("keyword")?.trim() ?? "";
  if (auth.status === "loading") return <p role="status">로그인 상태를 확인하는 중…</p>;
  return <FollowListContent key={`${props.userId}/${props.kind}/${props.page}/${auth.user?.id ?? "guest"}/${keyword}`} {...props} keyword={keyword} />;
}

function FollowListContent({ userId, kind, page, basePath, keyword }: Props & { keyword: string }) {
  const auth = useAuth();
  const router = useRouter();
  const [data, setData] = useState<FollowPageResponseDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [notice, setNotice] = useState("");
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [revision, setRevision] = useState(0);
  const [draft, setDraft] = useState(keyword);
  const actionInFlight = useRef(false);
  const mounted = useRef(false);
  const path = `${basePath}/${kind}`;
  const searchQuery = keyword ? `&keyword=${encodeURIComponent(keyword)}` : "";

  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    (keyword ? searchUsers(keyword, page, auth.accessToken, controller.signal)
      : getFollows(userId, kind, page, auth.accessToken, controller.signal))
      .then(result => {
        if (controller.signal.aborted) return;
        const lastPage = Math.max(0, result.totalPages - 1);
        if (page > lastPage) {
          router.replace(`${path}?page=${lastPage}${searchQuery}`, { scroll: false });
          return;
        }
        setData(result);
        setError("");
        setLoading(false);
      })
      .catch(reason => {
        if (controller.signal.aborted) return;
        setError(reason instanceof Error ? reason.message : "목록을 불러오지 못했습니다.");
        setLoading(false);
      });
    return () => controller.abort();
  }, [userId, kind, page, auth.accessToken, revision, path, router, keyword, searchQuery]);

  async function toggleFollow(user: FollowUserResponseDto) {
    if (!auth.accessToken || user.me || user.userId === auth.user?.id || actionInFlight.current) return;
    actionInFlight.current = true;
    setPendingId(user.userId);
    setActionError("");
    setNotice("");
    try {
      const result = await setFollowing(user.userId, !user.followedByMe, auth.accessToken);
      if (!mounted.current) return;
      setData(current => current ? { ...current, users: current.users.map(row => row.userId === result.targetUserId ? { ...row, followedByMe: result.followed } : row) } : current);
      setNotice(`${user.nickname}님을 ${result.followed ? "팔로우했습니다." : "언팔로우했습니다."}`);
      if (!keyword) {
        setLoading(true);
        setRevision(value => value + 1);
      }
    } catch (reason) {
      if (!mounted.current) return;
      setActionError(reason instanceof FollowApiError && reason.status === 401
        ? "로그인이 만료되었습니다. 다시 로그인해 주세요."
        : reason instanceof Error ? reason.message : "팔로우 상태를 변경하지 못했습니다.");
    } finally {
      actionInFlight.current = false;
      if (mounted.current) setPendingId(null);
    }
  }

  const firstVisiblePage = Math.max(0, Math.min(page - 2, (data?.totalPages ?? 1) - 5));
  const pages = Array.from({ length: Math.min(5, data?.totalPages ?? 0) }, (_, index) => firstVisiblePage + index);

  return <section className={styles.panel} aria-label="Friends">
    <div className={styles.heading}><h2>{keyword ? "유저 검색 결과" : "Friends"}</h2><span>{keyword ? "닉네임 순" : "최신 팔로우 순"}</span></div>
    {auth.status === "authenticated" && auth.accessToken && <UserSearch
      value={draft} accessToken={auth.accessToken} onChange={setDraft}
      onSearch={() => {
        const term = draft.trim();
        router.push(term ? `${path}?page=0&keyword=${encodeURIComponent(term)}` : path, { scroll: false });
      }}
      onSelect={user => router.push(`/profile/${user.userId}/following`)}
    />}
    {!keyword && <nav className={styles.tabs} aria-label="팔로우 목록 종류">
      <Link href={`${basePath}/following`} aria-current={kind === "following" ? "page" : undefined}>Following</Link>
      <span aria-hidden="true">/</span>
      <Link href={`${basePath}/followers`} aria-current={kind === "followers" ? "page" : undefined}>Followers</Link>
    </nav>}
    <p className={styles.notice} role="status">{notice}</p>
    {actionError && <p className={styles.error} role="alert">{actionError}</p>}
    {loading ? <p className={styles.empty} role="status">목록을 불러오는 중…</p> : error ? (
      <div className={styles.empty}><p className={styles.error} role="alert">{error}</p><button type="button" className={styles.retry} onClick={() => { setLoading(true); setRevision(value => value + 1); }}>다시 시도</button></div>
    ) : data && <>
      <p className={styles.count}>{keyword ? `“${keyword}” 검색 결과` : kind === "following" ? "팔로잉" : "팔로워"} {data.totalElements.toLocaleString()}명</p>
      {data.users.length === 0 ? <p className={styles.empty}>{keyword ? "검색 결과가 없습니다. 다른 닉네임으로 검색해 보세요." : kind === "following" ? "아직 팔로우하는 사용자가 없습니다." : "아직 팔로워가 없습니다."}</p> : (
        <ul className={styles.list}>{data.users.map(user => <li key={user.userId} className={styles.row}>
          <Link className={styles.identity} href={user.userId === auth.user?.id ? "/profile" : `/profile/${user.userId}`} aria-label={`${user.nickname}님의 프로필`}>
            <Avatar key={user.profileImageUrl} user={user} />
            <span className={styles.details}><strong>{user.nickname}{user.me && " (나)"}</strong>{!keyword && user.followedAt && <time dateTime={user.followedAt}>Since {user.followedAt.slice(0, 10).replaceAll("-", ".")}</time>}</span>
          </Link>
          {auth.status === "authenticated" && !user.me && user.userId !== auth.user?.id && <FollowButton
            user={user}
            pending={pendingId === user.userId}
            disabled={pendingId !== null}
            onClick={() => void toggleFollow(user)}
          />}
        </li>)}</ul>
      )}
      {data.totalPages > 0 && <nav className={styles.pagination} aria-label={keyword ? "유저 검색 결과 페이지" : "팔로우 목록 페이지"}>
        {page > 0 ? <Link href={`${path}?page=${page - 1}${searchQuery}`} scroll={false}>‹ 이전</Link> : <span aria-disabled="true">‹ 이전</span>}
        {pages.map(number => <Link key={number} href={`${path}?page=${number}${searchQuery}`} scroll={false} aria-label={`${number + 1}페이지`} aria-current={number === page ? "page" : undefined}>{number + 1}</Link>)}
        {data.hasNext ? <Link href={`${path}?page=${page + 1}${searchQuery}`} scroll={false}>다음 ›</Link> : <span aria-disabled="true">다음 ›</span>}
      </nav>}
    </>}
  </section>;
}
