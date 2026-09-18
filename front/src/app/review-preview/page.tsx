import ReviewPreviewClient from "./review-preview-client";
import styles from "./page.module.css";

type PreviewPageProps = {
  searchParams: Promise<{ gameId?: string }>;
};

export default async function ReviewPreviewPage({ searchParams }: PreviewPageProps) {
  const requestedGameId = Number((await searchParams).gameId);
  const gameId = Number.isSafeInteger(requestedGameId) && requestedGameId > 0
    ? requestedGameId
    : 6;

  return (
    <main className={styles.main}>
      <header className={styles.header}>
        <span>DEVELOPMENT PREVIEW</span>
        <h1>게임 #{gameId} 리뷰 영역</h1>
        <p>
          상세 페이지 담당자가 가져다 쓸 독립 컴포넌트입니다. 주소의
          <code>?gameId=6</code> 숫자를 바꾸면 다른 게임 리뷰를 확인할 수 있습니다.
        </p>
      </header>
      <ReviewPreviewClient gameId={gameId} />
    </main>
  );
}
