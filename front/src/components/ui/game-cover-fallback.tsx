// 게임 커버 이미지가 없거나 깨졌을 때 카드형 화면 전반에서 공유하는 대체 화면
// className은 카드 모양(가로세로 비율/테두리 등)을 이미지 쪽과 동일하게 맞춰야 하는 곳에서만 넘기면 된다.
export default function GameCoverFallback({ title, className }: { title: string; className?: string }) {
  return (
    <div className={className ? `cover-fallback ${className}` : "cover-fallback"}>
      <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M7 6h10c3 0 5 10 3 12-2 2-4-2-5-2H9c-1 0-3 4-5 2C2 16 4 6 7 6Z" />
        <path d="M6 11h5M8.5 8.5v5M16 10h.01M18 13h.01" />
      </svg>
      <span>{title}</span>
      <small>커버 준비 중</small>
    </div>
  );
}
