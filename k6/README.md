# 검색 부하 테스트

백엔드가 localhost:8080에서 실행 중이어야 합니다.

프로젝트 최상위 Git Bash에서:

```bash
docker compose -f compose.monitoring.yml up -d
bash k6/run-search.sh
```

기본값: 30 VU, 1분, Baldur 검색, 각 반복 뒤 1초 대기.
단일 사용자 비교: `bash k6/run-search.sh --vus 1 --duration 1m`
검색어 변경: `bash k6/run-search.sh -e KEYWORD=Mario`

Grafana: http://localhost:3001/d/gamelog-k6-search

실행마다 생성되는 testid를 대시보드에서 선택합니다. 시간 범위에 실행 시간이 포함되어야 합니다.
기존 JVM 대시보드와 기존 데이터 소스는 그대로 유지됩니다.

평균/p95/실패율/요청 수 카드는 선택 구간의 마지막 누적 값을 표시합니다.
각 실행은 별도 시리즈입니다. 실행 종료 후의 마지막 값으로 비교하세요.
처리량 그래프는 최근 20초의 평균으로 터미널 전체 실행 평균과 다릅니다.
검사 통과율은 요청당 상태 코드·응답 구조 검사 두 개의 결과입니다.
데이터가 없으면 0이 아닌 No data로 표시됩니다.

전송은 약 5초 간격이며, k6의 실험적 experimental-prometheus-rw 출력을 사용합니다.
과거 터미널 결과는 소급 수집되지 않습니다. 보존 기간은 Prometheus 설정에 따릅니다.
기존 p95 500ms 기준을 넘으면 종료 코드가 실패여도 지표는 전송됩니다.
대시보드 변경은 monitoring/grafana/dashboards/k6-search.json에서 관리합니다.

## OFFSET 개선 전후 비교

비교 화면: http://localhost:3001/d/gamelog-k6-offset

`benchmark`, `phase`, `keyword`, `page`, `testid`로 실행을 구분합니다.
같은 비교 묶음과 검색어, 페이지를 선택하고 개선 단계는 All로 두세요.
평균/p95는 실행별 마지막 누적 값이며 p95끼리 평균을 내지 않습니다.
시간 범위에 두 실행의 종료 시점까지 포함해야 합니다.
측정 대상은 **목록 API 전체(목록 SQL + 필요한 COUNT + 직렬화/대기 시간)**입니다.
SQL 하나의 시간과 혼동하지 마세요.

Git Bash에서 개선 전 백엔드와 기존 인덱스로 먼저 실행합니다.

```bash
BENCHMARK=offset-my-test PHASE=before KEYWORD=a bash k6/compare-search.sh
```

그 다음 개선 코드로 백엔드를 재시작하고 `monitoring/search-offset-index.sql`을
대상 DB에서 한 번 적용한 뒤 같은 조건으로 실행합니다.
**현재 코드를 양쪽 모두 실행하고 phase만 바꾸면 개선 전후 비교가 아닙니다.**

```bash
BENCHMARK=offset-my-test PHASE=after KEYWORD=a bash k6/compare-search.sh
```

각 페이지(0, 100)에 대해 5 VU/15초 예열 후 30 VU/1분 측정합니다.
페이지 크기는 20, 반복 사이 대기는 1초입니다. page=100은 OFFSET=2000입니다.
종료 유예시간은 기본 30초여서 전체 실행은 1분을 넘을 수 있습니다.
중단된 반복이 있으면 완료 요청만으로 성능을 단정하지 마세요.
기준 미달 종료 코드 99는 기록 후 다음 페이지로 진행하고, 다른 실행 오류는 중단합니다.
측정 중 빌드, 다른 부하 테스트, 데이터 수집/수정은 피하세요.
DB, JVM 옵션, 커넥션 풀, SQL 로그 설정을 같게 유지해야 합니다.
최대 처리량 테스트가 아니라 사용자 30명의 반복 검색 시나리오입니다.

개별 페이지 실행:

```bash
BENCHMARK=offset-my-test PHASE=after KEYWORD=Mario PAGE=1 SIZE=20 bash k6/run-search.sh
```

정확한 전체 실행 평균 RPS와 응답 시간은 `k6/results/<testid>.json`에도 저장됩니다.
이 폴더는 Git에서 제외합니다. Grafana의 최근 20초 RPS와 전체 평균은 다릅니다.
검색어 기본 정렬/장르·플랫폼 필터 없는 경로만 이번 최적화 대상입니다.
포함 검색의 인덱스 스캔, 정렬, COUNT 및 큰 OFFSET 비용은 여전히 남습니다.

### 2026-10-16 로컬 측정 환경

- DB: `gamelog_retry`, 전체 게임 284,294건, `a` 일치 216,841건.
- 실행 묶음: `offset-20261016`, JDK 25, JVM `-Xms512m -Xmx512m`.
- 두 버전 모두 `spring.jpa.show-sql=false`, `spring.jpa.hibernate.ddl-auto=none`,
  `spring.sql.init.mode=never`, 수집/번역 실행 비활성화, 기본 커넥션 풀 사용.
- 개선 전은 이번 작업 시작 시점의 미커밋 코드로 빌드한 JAR입니다.
  저장 위치: `.local-backups/search-offset/before.jar`. Git HEAD와 다를 수 있습니다.
- 개선 후는 정렬/페이지 선택 후 상세 조인 + `idx_games_release_title_rating` 인덱스입니다.
  인덱스는 로컬 DB에 실제 적용했고 기존 `idx_games_release_title`도 유지했습니다.
- 개선 전 page=100은 기본 유예시간 종료로 10회 중단되어 정상 비교에서 제외합니다.
  개선 전 page=0과 개선 후 실행은 각각의 완료 여부를 로그에서 확인합니다.
- 응답 데이터는 `a` page=0/100, `Baldur` page=0/1/2의 전체 JSON data가 전후 동일했습니다.
- 인덱스 적용 SQL은 자동 마이그레이션이 아닙니다. 다른 팀원의 DB에는 별도 적용해야 합니다.
