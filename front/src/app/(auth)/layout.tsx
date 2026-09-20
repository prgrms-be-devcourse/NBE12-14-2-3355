import Link from "next/link";
import styles from "./layout.module.css";

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className={styles.shell}>
      <Link href="/" className={styles.logo} aria-label="GameLog 홈">GameLog<span className="lime">.</span></Link>
      <div className={styles.card}>{children}</div>
    </div>
  );
}
