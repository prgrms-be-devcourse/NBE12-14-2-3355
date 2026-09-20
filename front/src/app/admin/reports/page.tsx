"use client";

import Link from "next/link";
import { useEffect, useState, type FormEvent } from "react";
import { getReviewReports, updateReviewReportStatus } from "@/features/reviews/api";
import type { ReportStatus, ReviewReportPage } from "@/features/reviews/types";
import styles from "./page.module.css";

const emptyPage: ReviewReportPage = {
  reports: [], page: 0, size: 20, totalElements: 0, totalPages: 0, hasNext: false,
};

const reportLabels: Record<ReportStatus, string> = {
  PENDING: "처리 대기",
  APPROVED: "승인",
  REJECTED: "반려",
};

const reviewStatusLabels = {
  ACTIVE: "공개 중",
  DELETED_BY_USER: "작성자 삭제",
  HIDDEN_BY_ADMIN: "관리자 숨김",
};
function getStoredAccessToken() {
  if (typeof window === "undefined") return "";
  return window.sessionStorage.getItem("gamelogAccessToken") ?? "";
}


export default function AdminReviewReportsPage() {
  const [accessToken, setAccessToken] = useState(getStoredAccessToken);
  const [tokenInput, setTokenInput] = useState(getStoredAccessToken);
  const [status, setStatus] = useState<ReportStatus | "">("PENDING");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<ReviewReportPage>(emptyPage);
  const [loading, setLoading] = useState(false);
  const [processingId, setProcessingId] = useState<number | null>(null);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);


  useEffect(() => {
    if (!accessToken) return;
    let active = true;
    void Promise.resolve().then(() => {
      if (active) {
        setLoading(true);
        setError("");
      }
    });
    getReviewReports(accessToken, page, 20, status || undefined)
      .then((data) => { if (active) setResult(data); })
      .catch((reason: unknown) => {
        if (active) setError(reason instanceof Error ? reason.message : "신고 목록을 불러오지 못했습니다.");
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [accessToken, page, refreshKey, status]);

  function connectToken(event: FormEvent) {
    event.preventDefault();
    const token = tokenInput.trim().replace(/^Bearer\s+/i, "");
    setAccessToken(token);
    setPage(0);
    setError("");
    if (token) window.sessionStorage.setItem("gamelogAccessToken", token);
    if (!token) setResult(emptyPage);
    else window.sessionStorage.removeItem("gamelogAccessToken");
  }

  async function processReport(reportId: number, nextStatus: "APPROVED" | "REJECTED") {
    if (!accessToken || processingId != null) return;
    const action = nextStatus === "APPROVED" ? "승인하고 리뷰를 숨김 처리" : "반려";
    if (!window.confirm(`이 신고를 ${action}할까요?`)) return;
    setProcessingId(reportId);
    setError("");
    try {
      await updateReviewReportStatus(reportId, nextStatus, accessToken);
      setRefreshKey((value) => value + 1);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "신고 상태를 변경하지 못했습니다.");
    } finally {
      setProcessingId(null);
    }
  }

  return <main className={styles.main}>
    <header className={styles.header}>
      <div><span>ADMIN CONSOLE</span><h1>리뷰 신고 관리</h1><p>신고 당시 리뷰 원문을 확인하고 승인 또는 반려합니다.</p></div>
      <Link href="/">게임 목록으로 돌아가기</Link>
    </header>

    <form className={styles.tokenForm} onSubmit={connectToken}>
      <label htmlFor="admin-token">관리자 accessToken</label>
      <input id="admin-token" type="password" value={tokenInput} onChange={(event) => setTokenInput(event.target.value)} placeholder="Bearer 없이 관리자 토큰 입력" />
      <button type="submit">연결</button>
    </form>

    <section className={styles.toolbar} aria-label="신고 필터">
      <strong>총 {result.totalElements.toLocaleString()}건</strong>
      <label>처리 상태
        <select value={status} onChange={(event) => { setStatus(event.target.value as ReportStatus | ""); setPage(0); }}>
          <option value="">전체</option><option value="PENDING">처리 대기</option><option value="APPROVED">승인</option><option value="REJECTED">반려</option>
        </select>
      </label>
    </section>

    {error && <div className={styles.error} role="alert">{error}</div>}
    {!accessToken ? <div className={styles.empty}>관리자 토큰을 연결해 주세요.</div>
      : loading ? <div className={styles.empty}>신고 목록을 불러오는 중…</div>
      : result.reports.length === 0 ? <div className={styles.empty}>조건에 맞는 신고가 없습니다.</div>
      : <div className={styles.list}>{result.reports.map((report) => <article className={styles.card} key={report.reportId}>
        <div className={styles.cardHeader}>
          <div><span className={`${styles.status} ${styles[report.status.toLowerCase()]}`}>{reportLabels[report.status]}</span><strong>신고 #{report.reportId}</strong></div>
          <time dateTime={report.createdDate}>{new Date(report.createdDate).toLocaleString("ko-KR")}</time>
        </div>
        <dl className={styles.meta}>
          <div><dt>리뷰</dt><dd>#{report.reviewId} · 작성자 #{report.reviewWriterId}</dd></div>
          <div><dt>신고자</dt><dd>#{report.reporterId}</dd></div>
          <div><dt>현재 상태</dt><dd>{reviewStatusLabels[report.reviewStatus]}</dd></div>
        </dl>
        <div className={styles.reason}><span>신고 사유</span><p>{report.reason}</p></div>
        <div className={styles.snapshot}>
          <div><span>신고 당시 리뷰</span><strong>{report.reviewRating == null ? "별점 없음" : `★ ${report.reviewRating.toFixed(1)}`}{report.reviewSpoiler ? " · 스포일러" : ""}</strong></div>
          <p>{report.reviewContent?.trim() || "별점만 남긴 리뷰입니다."}</p>
        </div>
        {report.status === "PENDING" && <div className={styles.actions}>
          <button type="button" className={styles.reject} onClick={() => processReport(report.reportId, "REJECTED")} disabled={processingId != null}>반려</button>
          <button type="button" className={styles.approve} onClick={() => processReport(report.reportId, "APPROVED")} disabled={processingId != null}>{processingId === report.reportId ? "처리 중…" : "승인 및 숨김"}</button>
        </div>}
      </article>)}</div>}

    {result.totalPages > 1 && <nav className={styles.pagination} aria-label="신고 목록 페이지 이동">
      <button type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>← 이전</button>
      <span>{page + 1} / {result.totalPages}</span>
      <button type="button" disabled={!result.hasNext} onClick={() => setPage((value) => value + 1)}>다음 →</button>
    </nav>}
  </main>;
}
