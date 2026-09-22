import styles from "./profile.module.css";

export default function ProfileLayout({ children }: { children: React.ReactNode }) {
  return <main className="main">
    <div className={styles.layout}>{children}</div>
  </main>;
}
