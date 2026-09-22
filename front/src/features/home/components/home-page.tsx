import Link from "next/link";
import PersonalizedGames from "@/features/recommendations/components/personalized-games";
import { PopularGamesSection, PopularReviewsSection } from "./popular-content";
import styles from "./home-page.module.css";

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
      <PopularGamesSection />
      <PopularReviewsSection />
  </main>;
}
