import { NextRequest } from "next/server";
import { relayAuthHeaders } from "@/lib/proxy";

type RouteContext = {
  params: Promise<{ path: string[] }>;
};

const allowedPath = /^(profile|profile\/[1-9]\d*|profile\/[1-9]\d*\/games|favorite-games|recentGames)$/;

async function proxy(
  request: NextRequest,
  context: RouteContext,
) {
  const { path } = await context.params;
  const endpoint = path.join("/");

  if (!allowedPath.test(endpoint)) {
    return Response.json(
      {
        data: null,
        msg: "요청한 경로를 찾을 수 없습니다.",
        resultCode: "404-0",
      },
      { status: 404 },
    );
  }

  const url = new URL(
    `/api/v1/library/games/${endpoint}`,
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

  if (request.headers.get("content-type")) {
    headers.set(
      "Content-Type",
      request.headers.get("content-type")!,
    );
  }

  try {
    const upstream = await fetch(url, {
      method: request.method,
      headers,
      body:
        request.method === "GET" || request.method === "HEAD"
          ? undefined
          : await request.text(),
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });

    const body = await upstream.text();

    const responseHeaders = new Headers({
      "Content-Type":
        upstream.headers.get("content-type") ||
        "application/json",
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
export const PUT = proxy;
