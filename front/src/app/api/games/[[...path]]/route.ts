import { NextRequest } from "next/server";

export async function GET(request: NextRequest, context: { params: Promise<{ path?: string[] }> }) {
  const { path = [] } = await context.params;
  const endpoint = path.join("/");
  if (!/^(page|filters|suggestions|popular|\d+(?:\/(?:reviews|related))?)$/.test(endpoint)) return Response.json({ msg: "요청한 경로를 찾을 수 없습니다." }, { status: 404 });
  const url = new URL(`/api/v1/games/${endpoint}`, process.env.BACKEND_URL || "http://localhost:8080");
  for (const key of ["keyword", "genreIds", "platformIds", "sort", "page", "size"]) {
    for (const value of request.nextUrl.searchParams.getAll(key)) url.searchParams.append(key, value);
  }
  try {
    const response = await fetch(url, { cache: "no-store", signal: AbortSignal.timeout(8000) });
    return Response.json(await response.json(), { status: response.status });
  } catch {
    return Response.json({ msg: "게임 서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요." }, { status: 502 });
  }
}
