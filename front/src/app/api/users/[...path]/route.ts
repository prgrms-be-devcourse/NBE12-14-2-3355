import { NextRequest } from "next/server";
import {relayAuthHeaders} from "@/lib/proxy";

type RouteContext = { params: Promise<{ path: string[] }> };

const allowedPath = /^(signup|login|logout|refresh|me(?:\/onboarding(?:\/skip)?|\/preferred-genres|\/preferred-games)?|check-email|check-nickname)$/;
const followListPath = /^[1-9]\d*\/(following|followers)$/;
const followActionPath = /^me\/following\/[1-9]\d*$/;
const publicUserPath = /^[1-9]\d*$/;

async function proxy(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  const endpoint = path.join("/");
  const isFollowList = endpoint === "search" || followListPath.test(endpoint);
  const isFollowAction = followActionPath.test(endpoint);
  const isPublicUser = publicUserPath.test(endpoint);
  if (!allowedPath.test(endpoint) && !isFollowList && !isFollowAction && !isPublicUser) {
    return Response.json({ data: null, msg: "요청한 경로를 찾을 수 없습니다.", resultCode: "404-0" }, { status: 404 });
  }

  if ((isPublicUser && request.method !== "GET") ||
    (isFollowList && request.method !== "GET") ||
      (isFollowAction && !["PUT", "DELETE"].includes(request.method))) {
    return Response.json({ data: null, msg: "허용되지 않은 요청 방식입니다.", resultCode: "405-0" }, {
      status: 405,
      headers: { Allow: isFollowList ? "GET" : "PUT, DELETE" },
    });
  }

  const url = new URL(`/api/v1/users/${endpoint}`, process.env.BACKEND_URL || "http://localhost:8080");
  request.nextUrl.searchParams.forEach((value, key) => url.searchParams.append(key, value));

  const authorization = request.headers.get("authorization");
  const headers = new Headers();
  if (authorization) headers.set("Authorization", authorization);
  if (request.headers.get("content-type")) headers.set("Content-Type", "application/json");
  const cookie = request.headers.get("cookie");
  if (cookie) headers.set("Cookie", cookie);

  try {
    const upstream = await fetch(url, {
      method: request.method,
      headers,
      body: request.method === "GET" || request.method === "HEAD" ? undefined : await request.text(),
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });
    const body = await upstream.text();
    const responseHeaders = new Headers({ "Content-Type": upstream.headers.get("content-type") || "application/json" });
    relayAuthHeaders(upstream, responseHeaders);
    return new Response(body, { status: upstream.status, headers: responseHeaders });
  } catch {
    return Response.json(
      { data: null, msg: "인증 서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.", resultCode: "502-1" },
      { status: 502 },
    );
  }
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
