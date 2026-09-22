import Link from "next/link";
import PersonalizedGames from "@/features/recommendations/components/personalized-games";
import styles from "./home-page.module.css";

function FavoriteSection({ type }: { type: "games" | "reviews" }) {
  const gameSection = type === "games";
  return <section className={styles.favoriteSection} aria-labelledby={`favorite-${type}-title`}>
    <div className={styles.heading}>
      <div>
        <span>{gameSection ? "COMMUNITY FAVORITES" : "MOST LOVED STORIES"}</span>
        <h2 id={`favorite-${type}-title`}>{gameSection ? "Favorite Games" : "Favorite Reviews"}</h2>
      </div>
      <p>{gameSection ? "GameLog 사용자들이 가장 많이 좋아한 게임" : "사용자들에게 가장 많은 공감을 받은 리뷰"}</p>
    </div>
    <div className={styles.emptyState}>
      <strong>{gameSection ? "좋아요가 많은 게임을 준비하고 있어요." : "좋아요가 많은 리뷰를 준비하고 있어요."}</strong>
      <p>{gameSection ? "게임 좋아요순 조회 API가 연결되면 이곳에 인기 게임이 표시됩니다." : "Popular Reviews API가 연결되면 이곳에 인기 리뷰가 표시됩니다."}</p>
    </div>
  </section>;
}

export default function HomePage() {
  return <main className="main">
      <section className="intro">
        <div>
          <span className="eyebrow"><span className="dot" /> YOUR NEXT FAVORITE</span>
          <h1>다음에 빠져들 <span>게임을 발견하세요.</span></h1>
          <p>익숙한 취향부터 새로운 세계까지, 나만의 다음 플레이를 찾아보세요.</p>
        </div>
        <Link className={styles.exploreLink} href="/games">전체 게임 둘러보기 <span aria-hidden="true">↗</span></Link>
      </section>
      <PersonalizedGames />
      <FavoriteSection type="games" />
      <FavoriteSection type="reviews" />
  </main>;
}
