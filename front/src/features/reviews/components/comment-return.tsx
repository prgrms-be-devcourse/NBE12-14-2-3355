"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/features/auth/auth-context";
import ReviewComments from "./review-comments";
import type { Review } from "../types";

// 원래 리뷰가 목록의 다음 페이지로 밀렸더라도 재로그인 후 초안에 접근할 수 있다.
export default function CommentReturn({ gameId }: { gameId?: string }) {
  const { accessToken } = useAuth();
  const [review, setReview] = useState<Review | null>(null);
  const [error, setError] = useState("");
  const [revealed, setRevealed] = useState(false);
  useEffect(() => {
    const id = new URLSearchParams(window.location.search).get("commentReview");
    if (!accessToken || !id || !/^[1-9]\d*$/.test(id)) return;
    const controller = new AbortController();
    fetch(`/api/review-api/reviews/${id}`, { headers: { Authorization: `Bearer ${accessToken}` }, signal: controller.signal, cache: "no-store" })
      .then(async response => {
        const body = await response.json();
        if (!response.ok) throw new Error(body.msg || "원래 리뷰를 불러오지 못했습니다.");
        if (gameId && String(body.data.gameId) !== gameId) throw new Error("이 게임의 리뷰가 아닙니다.");
        if (!controller.signal.aborted) setReview(body.data);
      }).catch(e => { if (!controller.signal.aborted) setError(e.message); });
    return () => controller.abort();
  }, [accessToken, gameId]);
  if (error) return <p role="alert">{error} 삭제되거나 숨겨진 리뷰에는 댓글을 등록할 수 없습니다.</p>;
  if (!review) return null;
  return <section aria-label="작성하던 댓글" style={{ padding: 20, marginBottom: 24, border: "1px solid #4a6340", borderRadius: 12 }}>
    <h2>작성하던 댓글</h2><p>{review.nickname}님의 {review.gameTitle} 리뷰</p>
    {review.spoiler && !revealed ? <button onClick={() => setRevealed(true)}>스포일러 리뷰 · 내용 보기</button>
      : <p style={{ whiteSpace: "pre-wrap", overflowWrap: "anywhere" }}>{review.content}</p>}
    <ReviewComments reviewId={review.reviewId} locked={review.spoiler && !revealed} autoOpen />
  </section>;
}
