import { NextRequest } from "next/server";
import { relayAuthHeaders } from "@/lib/proxy";

export async function POST(request: NextRequest) {
  const url = new URL("/api/v1/uploads/images", process.env.BACKEND_URL || "http://localhost:8080");

  const authorization = request.headers.get("authorization");
  const headers = new Headers();
  if (authorization) headers.set("Authorization", authorization);
  const contentType = request.headers.get("content-type");
  if (contentType) headers.set("Content-Type", contentType);

  try {
    const upstream = await fetch(url, {
      method: "POST",
      headers,
      body: await request.arrayBuffer(),
      cache: "no-store",
      signal: AbortSignal.timeout(15000),
    });
    const body = await upstream.text();
    const responseHeaders = new Headers({ "Content-Type": upstream.headers.get("content-type") || "application/json" });
    relayAuthHeaders(upstream, responseHeaders);
    return new Response(body, { status: upstream.status, headers: responseHeaders });
  } catch {
    return Response.json(
      { data: null, msg: "이미지 업로드 서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.", resultCode: "502-1" },
      { status: 502 },
    );
  }
}
