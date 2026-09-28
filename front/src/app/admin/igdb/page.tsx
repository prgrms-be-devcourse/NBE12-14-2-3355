"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useAuth } from "@/features/auth/auth-context";
import { acceptRefreshedToken } from "@/features/auth/api";
import styles from "./page.module.css";

type SyncState = {
  status: "IDLE" | "RUNNING" | "FAILED" | "SUCCEEDED";
  mode: "FULL" | "INCREMENTAL" | null;
  watermark: number | null;
  cutoff: number;
  processed: number;
  inserted: number;
  updated: number;
  deleted: number;
  skipped: number;
  leaseUntil: number;
  error: string | null;
};

async function request(token: string, method = "GET"): Promise<SyncState> {
  const response = await fetch("/api/admin/igdb-sync", {
    method, headers: { Authorization: `Bearer ${token}` }, cache: "no-store",
  });
  acceptRefreshedToken(response);
  const body = await response.json();
  if (!response.ok) throw new Error(body.msg || "동기화 요청에 실패했습니다.");
  return body.data;
}

function formatTime(epoch: number | null) {
  return epoch ? new Date(epoch * 1000).toLocaleString("ko-KR") : "아직 없음";
}

export default function IgdbAdminPage() {
  const auth = useAuth();
  const token = auth.accessToken;
  const isAdmin = auth.status === "authenticated" && auth.user?.role === "ADMIN";
  const [state, setState] = useState<SyncState | null>(null);
  const [error, setError] = useState("");
  const [sending, setSending] = useState(false);
  const [now, setNow] = useState(0);

  useEffect(() => {
    if (!isAdmin || !token) return;
    let active = true;
    let timer: ReturnType<typeof setTimeout>;
    async function poll() {
      try {
        const result = await request(token!);
        if (active) { setState(result); setError(""); setNow(Date.now() / 1000); }
      } catch (e) {
        if (active) setError(e instanceof Error ? e.message : "상태 조회 실패");
      } finally {
        if (active) timer = setTimeout(poll, 5000);
      }
    }
    void poll();
    return () => { active = false; clearTimeout(timer); };
  }, [isAdmin, token]);

  const running = state?.status === "RUNNING" && state.leaseUntil > now;
  const resume = state?.status === "FAILED" || (state?.status === "RUNNING" && !running);
  const label = running ? "동기화 실행 중…" : resume ? "중단된 동기화 이어서 실행" : state?.watermark ? "변경분 업데이트" : "최초 전체 동기화 시작";

  async function start() {
    if (!token || sending || running) return;
    setSending(true);
    setError("");
    try { setState(await request(token, "POST")); setNow(Date.now() / 1000); }
    catch (e) { setError(e instanceof Error ? e.message : "실행 실패"); }
    finally { setSending(false); }
  }

  if (!isAdmin) return <main className={styles.main}><h1>게임 데이터 관리</h1><p>{auth.status === "loading" ? "로그인 확인 중…" : "관리자만 접근할 수 있습니다."}</p><Link href="/login?next=/admin/igdb">로그인</Link></main>;

  return <main className={styles.main}>
    <header><span>ADMIN CONSOLE</span><h1>게임 데이터 업데이트</h1><p>1970년 1월 1일부터 실행 시점까지 출시된 게임을 관리합니다.</p></header>
    <section className={styles.panel}>
      <h2>{state?.watermark ? "변경된 게임과 새로 출시된 게임을 반영합니다" : "최초 전체 동기화로 최신 정보를 맞춥니다"}</h2>
      <p>출시일이 없거나 범위 밖으로 변경된 게임은 연결된 플레이 기록·리뷰와 함께 삭제됩니다. 설명 번역은 별도 작업입니다.</p>
      <button onClick={start} disabled={!state || sending || running}>{sending ? "실행 요청 중…" : label}</button>
      <p>창을 닫아도 서버에서 계속 실행됩니다. 실패하면 저장된 지점부터 이어서 실행할 수 있습니다.</p>
      {error && <p role="alert" className={styles.error}>{error}</p>}
      {state?.error && <p role="alert" className={styles.error}>{state.error}</p>}
    </section>
    {state && <section className={styles.panel} aria-live="polite">
      <h2>{running ? "실행 중" : resume ? "이어서 실행 가능" : state.status === "SUCCEEDED" ? "완료" : "실행 대기"}{state.mode ? ` · ${state.mode === "FULL" ? "전체 동기화" : "변경분 업데이트"}` : ""}</h2>
      <dl className={styles.metrics}>
        {([["확인", state.processed], ["신규 추가", state.inserted], ["기존 갱신", state.updated], ["삭제", state.deleted], ["범위 밖 제외", state.skipped]] as const).map(([name, value]) => <div key={name}><dt>{name}</dt><dd>{value.toLocaleString()}개</dd></div>)}
      </dl>
      <p>마지막 성공 기준: {formatTime(state.watermark)}</p>
      <p>현재 작업 기준: {formatTime(state.cutoff)}</p>
      <p>기존 갱신은 응답을 반영한 게임 수이며, 실제 값이 달라진 게임 수와는 다를 수 있습니다.</p>
    </section>}
    <Link href="/">게임 목록으로 돌아가기</Link>
  </main>;
}
