# 추천 API 부하 테스트 (macOS)

프로젝트 루트에서 실행합니다. 백엔드는 localhost:8080, Prometheus는 localhost:9090이 기본값입니다.

```sh
brew install k6
docker compose -f compose.monitoring.yml up -d
bash k6/run-recommendations.sh
```

기본 2분: 세 시나리오를 합쳐 VU 2 → 6 → 10 → 6 → 2 → 0, 반복 사이 1초 대기.
개별 요청은 60초 제한이며 진행 중 요청을 최대 30초 기다려 실제 종료는 더 늦을 수 있습니다.
첫 실행은 고정 데이터 선정, 테스트 계정 생성, 추천 결과 사전 확인으로 추가 시간이 듭니다.
수집·번역·다른 부하 테스트가 끝난 상태에서 비교하세요.

## 테스트 데이터와 시나리오

- `onboarding`: 새 테스트 계정, 선호 게임 최대 3개, 플레이 기록 없음.
- `record`: 별도 새 테스트 계정, 위와 같은 게임 최대 3개를 COMPLETED로 등록.
- `related`: 기준 게임의 상세 연관 추천. 인증 없이 조회.
- 각 실행은 필요한 테스트 계정 최대 2개와 연결 데이터를 로컬 DB에 추가합니다.
  개인 계정이나 기존 기록은 수정하지 않습니다. 테스트 계정은 결과 점검을 위해 남깁니다.
- 기본 기준 게임은 목록 첫 3개입니다. 재현 가능한 비교에는 GAME_IDS를 고정하세요.
  IGDB ID가 아니라 **이 DB의 games.id**를 사용합니다.
- 모든 경로에서 추천 목록이 비어 있지 않은지 사전 확인합니다. 빈 목록이면 다른 기준 게임을 선택하세요.
- 로그인/데이터 준비는 setup에서 수행하고 시나리오별 p95/p99에는 포함하지 않습니다.
- 두 계정을 여러 VU가 공유합니다. 10개 서로 다른 사용자 취향에 대한 시험은 아닙니다.

```sh
# 먼저 각 시나리오를 한 번씩 검증
SMOKE=true bash k6/run-recommendations.sh
# 세 시나리오 동시 실행 (최대 VU 합계 10)
GAME_IDS=47808,198812 bash k6/run-recommendations.sh
# 맞춤 추천만 / 연관 추천만
MODE=personalized bash k6/run-recommendations.sh
MODE=related RELATED_GAME_ID=47808 bash k6/run-recommendations.sh
```

## Grafana에서 확인

- Virtual Users: `k6_vus`, 범례 `VUs`.
- 시나리오별 `k6_http_req_duration_p95{scenario="onboarding"}` / p99.
  scenario는 `onboarding`, `record`, `related`이며 기존 수동 패널과 일치합니다.
- k6 2.3.0에서 `k6_vus`, `k6_http_req_duration_p95`의 실제 수신을 확인했습니다.
  Prometheus의 응답 시간은 초 단위, 실행 summary JSON의 http_req_duration은 ms 단위입니다.
- 같은 시간 범위에 여러 실행이 있으면 각 쿼리에 `testid="실행 시 출력된 ID"`를 추가해 분리하세요.
  서로 다른 실행/시나리오의 p95를 평균내면 전체 p95가 되지 않습니다.
- 기존 Request Rate/Error Rate/CPU/JVM 패널은 백엔드 지표를 계속 사용합니다.
  백엔드 전체 지표에는 데이터 준비 요청이나 다른 브라우저 요청도 포함될 수 있습니다.
- p95/p99는 실행 시작 이후 누적 분포입니다. 순간별 5초 구간의 p95는 아닙니다.
- 실패율 1% 미만, 추천 응답 검사 성공률 99% 초과를 기준으로 확인합니다.
  p99 60초는 요청 제한 수준의 가드이며 서비스 성능 목표를 달성했다는 뜻이 아닙니다.
- Prometheus 전송 실패는 k6의 콘솔 오류도 확인하세요. HTTP 검사 통과만으로 전송 성공을 보장하지 않습니다.
- 결과: `k6/results/<testid>.json`, 콘솔 기록: 같은 이름의 `.log` (Git 제외).
- 백엔드를 종료하거나 재시작한 실행은 정상 성능 비교에서 제외하세요.
  연결 실패 요청의 duration이 0으로 나타날 수 있어 응답 시간이 개선된 것으로 해석하면 안 됩니다.
- 반복 실행은 테스트 기록을 추가하므로 엄밀한 전후 비교에는 같은 DB 스냅샷과 게임 ID,
  JVM/SQL 로그/커넥션 풀 설정을 사용하세요.

## 로컬 검증 기록 (2026-10-19)

- k6 2.3.0 설치 및 JS/셸 구문, 세 시나리오의 실행 설정을 확인했습니다.
- 스모크 테스트에서 세 추천 경로 모두 HTTP 200 및 비어 있지 않은 결과를 확인했습니다.
- 당시 Prometheus는 remote-write 비활성 상태여서 첫 전송은 404로 실패했습니다.
  이후 기존 볼륨을 유지해 Prometheus 컨테이너를 재생성하고 수신 옵션 활성화를 확인했습니다.
- 다음 실행에서 `k6_vus=2` 및 onboarding p95 약 0.514초가 Prometheus에 저장된 것을 확인했습니다.
- 전체 부하 테스트는 도중 localhost:8080 연결이 끊겨 중단했습니다.
  최대 10 VU 전체 구간 완료 및 최종 성능 수치는 아직 검증하지 않았습니다.

가상 사용자는 k6, 요청량/서버 오류율은 서버 관측치이므로 숫자를 같은 지표로 혼동하지 마세요.
