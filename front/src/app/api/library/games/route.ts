import { NextRequest } from "next/server";
import { relayAuthHeaders } from "@/lib/proxy";

async function proxy(request: NextRequest) {
  const url = new URL(
    "/api/v1/library/games",
    process.env.BACKEND_URL || "http://localhost:8080",
  );

  request.nextUrl.searchParams.forEach((value, key) => {
    url.searchParams.append(key, value);
  });

  const headers = new Headers();

  const authorization = request.headers.get("authorization");
  if (authorization) {
    headers.set("Authorization", authorization);
  }

  const cookie = request.headers.get("cookie");
  if (cookie) {
    headers.set("Cookie", cookie);
  }

  try {
    const upstream = await fetch(url, {
      method: "GET",
      headers,
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });

    const body = await upstream.text();

    const responseHeaders = new Headers({
      "Content-Type":
        upstream.headers.get("content-type") || "application/json",
    });

    relayAuthHeaders(upstream, responseHeaders);

    return new Response(body, {
      status: upstream.status,
      headers: responseHeaders,
    });
  } catch {
    return Response.json(
      {
        data: null,
        msg: "게임 기록 서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.",
        resultCode: "502-1",
      },
      { status: 502 },
    );
  }
}

export const GET = proxy;