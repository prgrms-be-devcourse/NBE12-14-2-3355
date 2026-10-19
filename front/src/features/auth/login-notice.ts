// 다른 페이지에서 로그인 페이지로 이동시키며 안내 문구를 전달할 때 쓰는 sessionStorage 키
export const LOGIN_NOTICE_KEY = "gamelog:login-notice";

const messages: Record<string, string> = {
  "password-changed": "비밀번호가 변경되었습니다. 새 비밀번호로 다시 로그인해 주세요.",
};

// 저장된 안내를 지우지 않고 읽는다 (렌더 중 호출해도 안전하도록 부수효과 없음). 없거나 읽을 수 없으면 null.
export function peekLoginNotice(): { message: string; email: string } | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = sessionStorage.getItem(LOGIN_NOTICE_KEY);
    if (!raw) return null;
    const { reason, email } = JSON.parse(raw) as { reason?: string; email?: string };
    const message = reason ? messages[reason] : undefined;
    return message ? { message, email: email ?? "" } : null;
  } catch {
    return null;
  }
}

// 로그인에 성공하면 안내가 더 필요 없으므로 지운다.
export function clearLoginNotice() {
  try {
    sessionStorage.removeItem(LOGIN_NOTICE_KEY);
  } catch {
    // 저장소를 못 쓰는 환경이면 지울 것도 없다.
  }
}
