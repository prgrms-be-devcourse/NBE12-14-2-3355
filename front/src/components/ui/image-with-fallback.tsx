"use client";

import { useEffect, useState, type ReactNode } from "react";

type Props = {
  src: string | null | undefined;
  alt: string;
  fallback: ReactNode;
  className?: string;
  loading?: "lazy" | "eager";
};

// 게임 커버·유저 아바타 등에서 반복되던 "이미지 로드 실패 시 대체 화면" 로직을 공유한다.
export default function ImageWithFallback({ src, alt, fallback, className, loading = "lazy" }: Props) {
  const [broken, setBroken] = useState(false);

  useEffect(() => {
    setBroken(false);
  }, [src]);

  if (!src || broken) return <>{fallback}</>;

  // eslint-disable-next-line @next/next/no-img-element
  return <img className={className} src={src} alt={alt} loading={loading} onError={() => setBroken(true)} />;
}
