export function relayAuthHeaders(upstream: Response, responseHeaders: Headers) {
    for (const setCookie of upstream.headers.getSetCookie()) {
        responseHeaders.append("Set-Cookie", setCookie);
    }

    const newAccessToken = upstream.headers.get("New-Access-Token");
    if (newAccessToken) {
        responseHeaders.set("New-Access-Token", newAccessToken);
    }
}