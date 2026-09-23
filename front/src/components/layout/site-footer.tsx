import Link from "next/link";

export default function SiteFooter() {
  return <footer className="site-footer">
    <Link className="logo" href="/">GameLog<span className="lime">.</span></Link>
    <span>© 2026 GameLog. 한 게임씩, 나만의 이야기.</span>
    <Link className="site-footer-link" href="/games">전체 게임 보기 ↗</Link>
    <a
      className="site-footer-credit"
      href="https://www.flaticon.com/kr/free-icons/-"
      target="_blank"
      rel="noopener noreferrer"
    >
      Icons by sonnycandra · Flaticon
    </a>
  </footer>;
}
