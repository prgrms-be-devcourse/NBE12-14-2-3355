#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
export K6_PROMETHEUS_RW_SERVER_URL="${K6_PROMETHEUS_RW_SERVER_URL:-http://localhost:9090/api/v1/write}"
export K6_PROMETHEUS_RW_TREND_STATS='avg,p(95)'
export K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM=false
export K6_PROMETHEUS_RW_STALE_MARKERS=false
test_id="search-$(date -u +%Y%m%dT%H%M%S)-${RANDOM}"
echo "Grafana testid: $test_id"
mkdir -p k6/results
k6 run -o experimental-prometheus-rw --system-tags=name \
  --tag "testid=$test_id" --tag "phase=${PHASE:-manual}" \
  --tag "benchmark=${BENCHMARK:-manual}" --tag "keyword=${KEYWORD:-Baldur}" \
  --tag "page=${PAGE:-0}" --tag "size=${SIZE:-20}" \
  --summary-export "k6/results/$test_id.json" "$@" k6/search.js
