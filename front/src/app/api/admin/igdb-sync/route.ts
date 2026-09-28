import { NextRequest } from "next/server";
import { relayAuthHeaders } from "@/lib/proxy";

async function proxy(request: NextRequest) {
  const headers = new Headers();
  for (const name of ["authorization", "cookie"]) {
    const value = request.headers.get(name);
    if (value) headers.set(name, value);
  }
  try {
    const upstream = await fetch(new URL("/api/v1/admin/igdb-sync", process.env.BACKEND_URL || "http://localhost:8080"), {
      method: request.method, headers, cache: "no-store", signal: AbortSignal.timeout(10000),
    });
    const responseHeaders = new Headers({ "Content-Type": "application/json", "Cache-Control": "no-store" });
    relayAuthHeaders(upstream, responseHeaders);
    return new Response(await upstream.text(), { status: upstream.status, headers: responseHeaders });
  } catch {
    return Response.json({ msg: "서버 응답을 확인하지 못했습니다. 상태를 확인한 후 다시 시도하세요." }, { status: 502 });
  }
}

export const GET = proxy;
export const POST = proxy;
