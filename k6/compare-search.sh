#!/usr/bin/env bash
# 같은 BENCHMARK로 before/after를 각각 실행합니다. 백엔드/인덱스는 별도로 전환합니다.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${BENCHMARK:?BENCHMARK를 같은 비교 이름으로 지정하세요}"
: "${PHASE:?PHASE=before 또는 after를 지정하세요}"
[[ "$PHASE" == before || "$PHASE" == after ]] || exit 2
export KEYWORD="${KEYWORD:-a}" SIZE=20
for page in 0 100; do
  export PAGE="$page"
  echo "Warmup: $PHASE keyword=$KEYWORD page=$PAGE"
  k6 run --vus 5 --duration 15s --no-thresholds k6/search.js
  # 성능 기준 미달(99)은 측정 결과이며 다음 페이지도 계속 측정합니다.
  result=0
  bash k6/run-search.sh --vus 30 --duration 1m || result=$?
  if [[ "$result" != 0 && "$result" != 99 ]]; then exit "$result"; fi
done
