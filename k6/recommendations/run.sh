#!/usr/bin/env bash
set -euo pipefail

# 두 JS 테스트의 공통 실행 진입점. 새 터미널에서도 P95/P99를 함께 전송한다.
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
case "${1:-}" in
  personalized) script="$script_dir/personalized-recommendations.js" ;;
  related) script="$script_dir/related-recommendations.js" ;;
  *)
    echo '사용법: bash k6/recommendations/run.sh personalized|related [k6 실행 옵션]' >&2
    exit 2
    ;;
esac
shift

# 결과 파일 등 상대 경로도 추천 폴더 안에서 관리한다.
cd -- "$script_dir"

if ! command -v k6 >/dev/null 2>&1; then
  echo 'k6를 먼저 설치하세요: brew install k6' >&2
  exit 1
fi

export K6_NO_USAGE_REPORT=true
export K6_PROMETHEUS_RW_SERVER_URL="${K6_PROMETHEUS_RW_SERVER_URL:-http://localhost:9090/api/v1/write}"
# summaryTrendStats는 콘솔만 제어한다. Prometheus 출력은 별도로 설정해야 한다.
# 이전 터미널에 p(99)만 지정되어 있어도 아래 값으로 덮어쓴다.
export K6_PROMETHEUS_RW_TREND_STATS='avg,p(95),p(99)'
export K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM=false
export K6_PROMETHEUS_RW_PUSH_INTERVAL=5s
export K6_PROMETHEUS_RW_STALE_MARKERS=true

echo 'Prometheus 전송: VUs, 요청 수, 실패율, 응답 시간 avg/P95/P99 (5초 간격)'
exec k6 run --out experimental-prometheus-rw "$script" "$@"
