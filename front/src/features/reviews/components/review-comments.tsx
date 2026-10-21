"use client";

import Link from "next/link";
import { useCallback, useEffect, useId, useRef, useState } from "react";
import { useAuth } from "@/features/auth/auth-context";
import { ReviewApiError } from "../api";
import { commentRequest, type Comment, type CommentPage } from "../comments-api";
import ImageWithFallback from "@/components/ui/image-with-fallback";
import styles from "./review-comments.module.css";

type Draft = { content: string; spoiler: boolean; parentId: number | null; editId: number | null; target: string };
const blank: Draft = { content: "", spoiler: false, parentId: null, editId: null, target: "" };

export default function ReviewComments({ reviewId, locked = false, autoOpen = false }: { reviewId: number; locked?: boolean; autoOpen?: boolean }) {
  const auth = useAuth();
  const inputId = useId();
  const [open, setOpen] = useState(autoOpen);
  const [count, setCount] = useState<number | null>(null);
  const [draft, setDraft] = useState<Draft>(blank);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [version, setVersion] = useState(0);
  const [fresh, setFresh] = useState<Comment | null>(null);
  const [expandedParent, setExpandedParent] = useState<number | null>(null);
  const [focusedParent, setFocusedParent] = useState<Comment | null>(null);
  const guard = useRef(false);
  const editor = useRef<HTMLTextAreaElement>(null);
  const alive = useRef(true);
  const storageKey = `gamelog:comment:${auth.user?.id}:${reviewId}`;
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);

  useEffect(() => {
    if (!autoOpen || !auth.user) return;
    Promise.resolve().then(() => {
      try {
        const raw = sessionStorage.getItem(storageKey);
        if (!raw || !alive.current) return;
        const saved = JSON.parse(raw) as Draft;
        if (typeof saved.content !== "string" || saved.content.length > 1000) return;
        setDraft(saved);
        setExpandedParent(saved.parentId);
        if (saved.parentId) {
          commentRequest<Comment>(reviewId, `/${saved.parentId}`, auth.accessToken).then(parent => {
            if (alive.current) setFocusedParent(parent);
          }).catch(() => { if (alive.current) setError("답글 대상을 불러오지 못했습니다. 삭제 여부를 확인해 주세요."); });
        }
      } catch { /* 저장소가 차단되면 빈 입력창으로 계속 진행한다. */ }
    });
  }, [autoOpen, auth.user, auth.accessToken, reviewId, storageKey]);

  const recount = useCallback(() => {
    commentRequest<number>(reviewId, "/count").then(value => { if (alive.current) setCount(value); }).catch(() => {});
  }, [reviewId]);
  useEffect(() => { recount(); }, [recount, version]);

  function login() {
    try { if (draft.content) sessionStorage.setItem(storageKey, JSON.stringify(draft)); } catch { /* 입력은 현재 화면에 유지된다. */ }
    const url = new URL(window.location.href);
    url.searchParams.set("commentReview", String(reviewId));
    // 인증 상태 변경으로 부모가 먼저 리다이렉트해도 초안 복귀 URL을 확정하기 위해 문서를 이동한다.
    // eslint-disable-next-line @next/next/no-location-assign-relative-destination
    window.location.assign(`/login?next=${encodeURIComponent(url.pathname + url.search)}`);
  }

  async function mutate<T>(operation: (token: string) => Promise<T>) {
    if (!auth.accessToken) { login(); throw new Error("로그인 후 다시 시도해 주세요."); }
    try { return await operation(auth.accessToken); }
    catch (reason) {
      if (!(reason instanceof ReviewApiError) || reason.status !== 401) throw reason;
      // refresh가 인증 상태를 바꿔 부모 화면을 제거하기 전에 보관한다.
      try { if (draft.content) sessionStorage.setItem(storageKey, JSON.stringify(draft)); } catch {}
      const token = await auth.refresh();
      if (token) {
        try { return await operation(token); }
        catch (retryError) {
          if (!(retryError instanceof ReviewApiError) || retryError.status !== 401) throw retryError;
          await auth.logout();
        }
      }
      login();
      throw new Error("로그인 후 다시 등록해 주세요.");
    }
  }

  async function action(work: () => Promise<void>) {
    if (guard.current) return;
    guard.current = true; setBusy(true); setError("");
    try { await work(); }
    catch (reason) { if (alive.current) setError(reason instanceof Error ? reason.message : "요청에 실패했습니다."); }
    finally { guard.current = false; if (alive.current) setBusy(false); }
  }
  function edit(value: Draft) {
    setDraft(value);
    editor.current?.focus();
  }
  async function submit() {
    await action(async () => {
      const item = await mutate(token => commentRequest<Comment>(reviewId, draft.editId ? `/${draft.editId}` : "", token,
        draft.editId ? "PUT" : "POST", { content: draft.content, spoiler: draft.spoiler, parentId: draft.parentId }));
      if (!alive.current) return;
      if (!draft.editId) { setFresh(item); setExpandedParent(item.parentId); setVersion(v => v + 1); }
      else { setFresh(null); setVersion(v => v + 1); }
      setDraft(blank);
      try { sessionStorage.removeItem(storageKey); } catch {}
      recount();
    });
  }

  return <div className={styles.comments}>
    <button type="button" disabled={locked} aria-expanded={open && !locked} onClick={() => { setOpen(!open); if (!open) { setVersion(v => v + 1); setFresh(null); } }}>
      댓글{count == null ? "" : ` ${count}개`}{locked ? " · 리뷰 내용 공개 후 보기" : ""}
    </button>
    {open && !locked && <section aria-label="리뷰 댓글">
      <div className={styles.toolbar}><strong>댓글 {count ?? ""}</strong><button disabled={busy} onClick={() => { setVersion(v => v + 1); setFresh(null); }}>새로고침</button></div>
      {error && <p role="alert" className={styles.error}>{error}</p>}
      <CommentList key={`${version}:${auth.user?.id}`} reviewId={reviewId} token={auth.accessToken} parentId={null}
        fresh={fresh} focusedParent={focusedParent} expandedParent={expandedParent} busy={busy} onCount={setCount}
        onReply={c => edit({ ...blank, parentId: c.id, target: c.nickname ?? "" })}
        onEdit={c => edit({ content: c.content, spoiler: c.spoiler, editId: c.id, parentId: c.parentId, target: "" })}
        onDelete={c => { if (window.confirm("댓글을 삭제할까요?")) void action(async () => {
          await mutate(token => commentRequest(reviewId, `/${c.id}`, token, "DELETE"));
          setFresh(null); setFocusedParent(null); setVersion(v => v + 1); recount();
        }); }}
        onLike={c => mutate(token => commentRequest<Comment>(reviewId, `/${c.id}/likes`, token, c.liked ? "DELETE" : "PUT"))}
      />
      {auth.status === "authenticated" ? <form className={styles.editor} onSubmit={e => { e.preventDefault(); void submit(); }}>
        <label htmlFor={inputId}>{draft.editId ? "댓글 수정" : draft.parentId ? `${draft.target}님에게 답글` : "댓글 작성"}</label>
        <textarea id={inputId} ref={editor} maxLength={1000} rows={3} value={draft.content} disabled={busy}
          onChange={e => setDraft({ ...draft, content: e.target.value })} placeholder="게임에 대한 이야기를 나눠보세요." required />
        <div className={styles.toolbar}><label><input type="checkbox" checked={draft.spoiler} disabled={busy}
          onChange={e => setDraft({ ...draft, spoiler: e.target.checked })} /> 스포일러 포함</label><small>{draft.content.length} / 1000</small></div>
        <div className={styles.toolbar}>
          <button type="button" disabled={busy} onClick={() => { setDraft(blank); try { sessionStorage.removeItem(storageKey); } catch {} }}>취소</button>
          <button disabled={busy || !draft.content.trim()}>{busy ? "처리 중…" : draft.editId ? "수정 저장" : "등록"}</button>
        </div>
      </form> : <button onClick={login}>로그인하고 댓글 작성</button>}
    </section>}
  </div>;
}

type ListProps = {
  reviewId: number; parentId: number | null; token: string | null; fresh: Comment | null;
  focusedParent: Comment | null;
  expandedParent: number | null; busy: boolean; onCount: (n: number) => void;
  onReply: (c: Comment) => void; onEdit: (c: Comment) => void; onDelete: (c: Comment) => void;
  onLike: (c: Comment) => Promise<Comment>;
};
function CommentList(props: ListProps) {
  const { reviewId, parentId, token, fresh, onCount } = props;
  const [rows, setRows] = useState<Comment[]>([]);
  const [cursor, setCursor] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const controller = useRef<AbortController | null>(null);
  const lock = useRef(false);
  const initialToken = useRef(token);
  const query = useCallback((after?: number | null) => {
    const params = new URLSearchParams();
    if (parentId) params.set("parentId", String(parentId));
    if (after) params.set("after", String(after));
    return `?${params}`;
  }, [parentId]);
  useEffect(() => {
    const abort = new AbortController(); controller.current = abort;
    commentRequest<CommentPage>(reviewId, query(), initialToken.current, "GET", undefined, abort.signal)
      .then(data => { if (!abort.signal.aborted) { setRows(data.comments); setCursor(data.nextCursor); onCount(data.totalCount); } })
      .catch(e => { if (!abort.signal.aborted) setError(e.message); })
      .finally(() => { if (!abort.signal.aborted) setLoading(false); });
    return () => abort.abort();
  }, [reviewId, query, attempt, onCount]);
  async function more() {
    if (lock.current) return;
    lock.current = true; setLoading(true); setError("");
    try {
      const data = await commentRequest<CommentPage>(reviewId, query(cursor), token, "GET", undefined, controller.current?.signal);
      if (controller.current?.signal.aborted) return;
      setRows(previous => [...previous, ...data.comments.filter(c => !previous.some(p => p.id === c.id))]);
      setCursor(data.nextCursor); onCount(data.totalCount);
    } catch (e) { if (!controller.current?.signal.aborted) setError(e instanceof Error ? e.message : "조회에 실패했습니다."); }
    finally { lock.current = false; if (!controller.current?.signal.aborted) setLoading(false); }
  }
  const newHere = fresh?.parentId === parentId && !rows.some(c => c.id === fresh.id) ? fresh : null;
  return <div className={parentId ? styles.replies : undefined}>
    {parentId === null && props.focusedParent && !rows.some(c => c.id === props.focusedParent?.id) &&
      <div className={styles.fresh}><small>작성하던 답글의 원댓글</small><CommentRow {...props} comment={props.focusedParent} /></div>}
    {newHere && <div className={styles.fresh}><small>방금 작성한 {parentId ? "답글" : "댓글"}</small><CommentRow {...props} comment={newHere} /></div>}
    {rows.map(c => <CommentRow {...props} key={c.id} comment={c} />)}
    {!loading && !error && rows.length === 0 && !newHere && <p className={styles.muted}>아직 {parentId ? "답글" : "댓글"}이 없습니다.</p>}
    {loading && <p role="status">댓글을 불러오는 중…</p>}
    {error && <p role="alert">{error} <button disabled={loading} onClick={() => {
      if (rows.length) void more(); else { setLoading(true); setError(""); setAttempt(v => v + 1); }
    }}>다시 시도</button></p>}
    {cursor && !error && <button disabled={loading} onClick={() => void more()}>{parentId ? "답글 더 보기" : "댓글 더 보기"}</button>}
  </div>;
}
function CommentRow(props: ListProps & { comment: Comment }) {
  const { comment: original, onReply, onEdit, onDelete, onLike, busy } = props;
  const [updated, setUpdated] = useState<Comment | null>(null);
  const c = updated ?? original;
  const [revealed, setRevealed] = useState(false);
  const [showReplies, setShowReplies] = useState(false);
  const [closedFreshId, setClosedFreshId] = useState<number | null>(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const guard = useRef(false);
  const expanded = showReplies || (props.expandedParent === c.id && closedFreshId !== (props.fresh?.id ?? 0));
  const newReply = props.fresh?.parentId === c.id;
  return <div className={styles.row}>
    {c.deleted ? <p className={styles.muted}>삭제된 댓글입니다.</p> : <>
      <div className={styles.identity}>
        <Link href={`/profile/${c.userId}`} className={styles.avatar}><ImageWithFallback src={c.profileImageUrl} alt="" fallback={<span>{c.nickname?.slice(0, 1)}</span>} /></Link>
        <Link href={`/profile/${c.userId}`}>{c.nickname}</Link>
        {c.author && <span className={styles.badge}>리뷰 작성자</span>}
      </div>
      {c.spoiler && !revealed ? <button className={styles.spoiler} onClick={() => setRevealed(true)}>스포일러 댓글 · 내용 보기</button> : <p className={styles.content}>{c.content}</p>}
      <div className={styles.meta}><time dateTime={c.createdDate}>{c.createdDate.slice(0, 16).replace("T", " ")}</time>{c.edited && <span>수정됨</span>}
        {!c.parentId && <button disabled={busy} onClick={() => onReply(c)}>답글 달기</button>}
        {c.mine && <><button disabled={busy} onClick={() => onEdit(c)}>수정</button><button disabled={busy} onClick={() => onDelete(c)}>삭제</button></>}
        <button aria-pressed={c.liked} disabled={busy || pending} onClick={async () => {
          if (guard.current) return;
          guard.current = true; setPending(true); setError("");
          try { setUpdated(await onLike(c)); } catch (e) { setError(e instanceof Error ? e.message : "좋아요를 변경하지 못했습니다."); }
          finally { guard.current = false; setPending(false); }
        }}>{c.liked ? "♥" : "♡"} 좋아요 {c.likeCount}</button>
      </div>
      {error && <p role="alert" className={styles.error}>{error}</p>}
    </>}
    {!c.parentId && (c.replyCount > 0 || newReply) && <button aria-expanded={expanded} onClick={() => {
      setShowReplies(!expanded); if (expanded) setClosedFreshId(props.fresh?.id ?? 0);
    }}>답글 {c.replyCount}개 {expanded ? "접기" : "보기"}</button>}
    {!c.parentId && expanded && <CommentList {...props} parentId={c.id} />}
  </div>;
}
