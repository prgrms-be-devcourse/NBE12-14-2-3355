import { NextRequest } from "next/server";

type RouteContext = { params: Promise<{ path: string[] }> };

const allowedPath = /^(games\/\d+\/reviews(?:\/me)?|users\/\d+\/reviews|reviews\/\d+(?:\/likes(?:\/count)?|\/reports)?|admin\/review-reports(?:\/\d+\/status)?)$/;

async function proxy(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  const endpoint = path.join("/");
  if (!allowedPath.test(endpoint)) {
    return Response.json({ data: null, msg: "요청한 경로를 찾을 수 없습니다.", resultCode: "404-0" }, { status: 404 });
  }

  const url = new URL(`/api/v1/${endpoint}`, process.env.BACKEND_URL || "http://localhost:8080");
  request.nextUrl.searchParams.forEach((value, key) => url.searchParams.append(key, value));

  const authorization = request.headers.get("authorization");
  const headers = new Headers();
  if (authorization) headers.set("Authorization", authorization);
  if (request.headers.get("content-type")) headers.set("Content-Type", "application/json");

  try {
    const response = await fetch(url, {
      method: request.method,
      headers,
      body: request.method === "GET" || request.method === "HEAD" ? undefined : await request.text(),
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });
    const body = await response.text();
    return new Response(body, {
      status: response.status,
      headers: { "Content-Type": response.headers.get("content-type") || "application/json" },
    });
  } catch {
    return Response.json(
      { data: null, msg: "리뷰 서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.", resultCode: "502-1" },
      { status: 502 },
    );
  }
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const DELETE = proxy;
