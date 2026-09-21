import { NextRequest } from "next/server";
import { relayAuthHeaders } from "@/lib/proxy";

// 맞춤 추천 요청의 인증 정보와 자동 재발급 응답을 전달
export async function GET(request: NextRequest) {
  const headers = new Headers();
  for (const name of ["authorization", "cookie"]) {
    const value = request.headers.get(name);
    if (value) headers.set(name, value);
  }

  try {
    const url = new URL("/api/v1/games/recommendations/personalized", process.env.BACKEND_URL || "http://localhost:8080");
    const upstream = await fetch(url, { headers, cache: "no-store", signal: AbortSignal.timeout(8000) });
    const responseHeaders = new Headers({
      "Content-Type": upstream.headers.get("content-type") || "application/json",
      "Cache-Control": "private, no-store",
    });
    relayAuthHeaders(upstream, responseHeaders);
    return new Response(await upstream.text(), { status: upstream.status, headers: responseHeaders });
  } catch {
    return Response.json({ msg: "추천 게임을 불러오지 못했어요." }, { status: 502 });
  }
}
