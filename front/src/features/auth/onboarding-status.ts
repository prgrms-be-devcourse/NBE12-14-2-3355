import type { UserDto } from "./types";

// user.onboardingCompleted는 백엔드가 아직 안 내려주는 필드라 지금은 항상 undefined다.
// undefined === false/true는 둘 다 false이므로, 필드가 없는 지금은 두 함수 모두
// "온보딩 필요 없음"/"온보딩 완료 아님"으로 안전하게 판단하고 아무 것도 강제하지 않는다.
// 백엔드가 필드를 내려주기 시작하면 이 파일은 수정 없이 그대로 정상 동작한다.

export function needsOnboarding(user: Pick<UserDto, "onboardingCompleted">): boolean {
  return user.onboardingCompleted === false;
}

export function isOnboarded(user: Pick<UserDto, "onboardingCompleted">): boolean {
  return user.onboardingCompleted === true;
}
