#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v k6 >/dev/null || { echo '먼저 brew install k6를 실행하세요.' >&2; exit 1; }
export K6_NO_USAGE_REPORT=true
export K6_PROMETHEUS_RW_SERVER_URL="${K6_PROMETHEUS_RW_SERVER_URL:-http://localhost:9090/api/v1/write}"
export K6_PROMETHEUS_RW_TREND_STATS='avg,p(95),p(99)'
export K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM=false
export K6_PROMETHEUS_RW_PUSH_INTERVAL=5s
# 끝난 실행은 stale 처리하여 이전 VU와 새 VU가 겹쳐 보이지 않게 한다.
export K6_PROMETHEUS_RW_STALE_MARKERS=true
test_id="recommend-$(date -u +%Y%m%dT%H%M%S)-${RANDOM}"
mkdir -p k6/results
echo "Grafana testid: $test_id"
set +e
k6 run --quiet -o experimental-prometheus-rw \
  --system-tags=status,method,name,scenario,expected_response \
  --tag "testid=$test_id" \
  --summary-export "k6/results/$test_id.json" \
  "$@" k6/recommendations.js 2>&1 | tee "k6/results/$test_id.log"
run_status=${PIPESTATUS[0]}
set -e
if grep -Eq 'Failed to send the time series data|marking time series as stale failed' "k6/results/$test_id.log"; then
  echo 'Prometheus 전송 실패: 수신 옵션과 연결 상태를 확인하세요. 이번 실행은 전송 성공으로 판단하지 마세요.' >&2
  exit 1
fi
exit "$run_status"
