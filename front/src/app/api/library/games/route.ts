import { NextRequest } from "next/server";
import { relayAuthHeaders } from "@/lib/proxy";

export async function GET(request: NextRequest) {
  const url = new URL("/api/v1/library/games", process.env.BACKEND_URL || "http://localhost:8080");
  for (const key of ["status", "sort", "keyword", "platformIds", "genreIds", "page", "size"]) {
    for (const value of request.nextUrl.searchParams.getAll(key)) url.searchParams.append(key, value);
  }
  const headers = new Headers();
  for (const key of ["authorization", "cookie"]) {
    const value = request.headers.get(key);
    if (value) headers.set(key, value);
  }
  try {
    const upstream = await fetch(url, { headers, cache: "no-store", signal: AbortSignal.timeout(8000) });
    const responseHeaders = new Headers({ "Content-Type": upstream.headers.get("content-type") || "application/json" });
    relayAuthHeaders(upstream, responseHeaders);
    return new Response(await upstream.text(), { status: upstream.status, headers: responseHeaders });
  } catch {
    return Response.json({ msg: "게임 기록 서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요." }, { status: 502 });
  }
}
