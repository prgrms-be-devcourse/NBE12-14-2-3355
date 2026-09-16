import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = {
  title: "GameLog | 다음에 빠져들 게임을 발견하세요",
  description: "장르와 플랫폼으로 탐색하고, 다음에 플레이할 게임을 찾아보세요.",
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="ko"><body>{children}</body></html>;
}
