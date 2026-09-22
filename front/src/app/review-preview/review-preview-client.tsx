"use client";

import { useState, type FormEvent } from "react";
import GameReviewSection from "@/features/reviews/components/game-review-section";
import styles from "./page.module.css";

export default function ReviewPreviewClient({ gameId }: { gameId: number }) {
  const [draftToken, setDraftToken] = useState("");
  const [accessToken, setAccessToken] = useState<string>();
  const [notice, setNotice] = useState("");

  function connect(event: FormEvent) {
    event.preventDefault();
    const normalized = draftToken.trim().replace(/^Bearer\s+/i, "");
    if (!normalized) {
      setNotice("Postman에서 발급받은 accessToken을 입력해 주세요.");
      return;
    }
    setAccessToken(normalized);
    setDraftToken("");
    setNotice("토큰이 연결됐습니다. 이제 리뷰 작성과 좋아요를 테스트할 수 있어요.");
  }

  function disconnect() {
    setAccessToken(undefined);
    setDraftToken("");
    setNotice("토큰 연결을 해제했습니다.");
  }

  return (
    <>
      <section className={styles.authPanel} aria-labelledby="preview-token-title">
        <div>
          <span>LOCAL AUTH TEST</span>
          <h2 id="preview-token-title">개발용 로그인 토큰</h2>
          <p>토큰은 현재 화면의 메모리에만 보관되며 새로고침하면 사라집니다.</p>
        </div>
        {accessToken ? (
          <div className={styles.connected}>
            <strong><i /> 토큰 연결됨</strong>
            <button type="button" onClick={disconnect}>연결 해제</button>
          </div>
        ) : (
          <form onSubmit={connect} className={styles.tokenForm}>
            <label htmlFor="preview-access-token">Access Token</label>
            <div>
              <input
                id="preview-access-token"
                type="password"
                value={draftToken}
                onChange={(event) => setDraftToken(event.target.value)}
                placeholder="Bearer 없이 accessToken만 붙여 넣으세요"
                autoComplete="off"
                spellCheck={false}
              />
              <button type="submit">토큰 연결</button>
            </div>
          </form>
        )}
        {notice && <p className={styles.notice} role="status">{notice}</p>}
      </section>

      <GameReviewSection
        gameId={gameId}
        accessToken={accessToken}
        onLoginRequired={() => setNotice("위 입력창에 accessToken을 먼저 연결해 주세요.")}
      />
    </>
  );
}
