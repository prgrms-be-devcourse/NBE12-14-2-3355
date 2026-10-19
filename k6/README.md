# 검색 부하 테스트

## 로컬 단일 요청 지연 비교 (PowerShell)

`measure-search.ps1`은 자동완성 `a`/`al`, 검색어 없는 목록 page=0/100(size=15)을
각각 1회 예열하고 5회 순차 측정합니다. page=100의 OFFSET은 1500입니다.
응답 시간 중앙값이 임시 진단 기준인 500ms를 초과하거나 요청이 실패하면 실패 종료합니다.
이 기준은 서비스 SLO가 아니며 `-MaxMilliseconds`로 변경할 수 있습니다.

```powershell
powershell -NoProfile -File k6/measure-search.ps1 -OutputPath k6/results/search-before.json
# 수정한 백엔드를 재시작한 뒤, 같은 DB와 부하 조건에서 실행합니다.
powershell -NoProfile -File k6/measure-search.ps1 -OutputPath k6/results/search-after.json -ComparePath k6/results/search-before.json
```

`-ComparePath`는 응답의 전체 `data`(후보/목록 순서, 총 개수, 페이지 메타데이터 포함)가
기준 결과와 같은지도 검사합니다. 비교 중 데이터가 바뀌면 동일성 검사가 실패할 수 있습니다.
Next.js 프록시를 포함하려면 `-BaseUrl http://localhost:3000/api/games`를 지정합니다.
브라우저 입력 대기 시간이나 렌더링은 측정하지 않습니다.

2026-10-19 로컬 MySQL 284,294건에서 기존 8080 서버와 수정 빌드의 8081 서버를
같은 DB로 순차 비교한 중앙값: 자동완성 a 1135.9→157.9ms, al 1027.5→115.8ms,
전체 목록 page=0 1119.4→147.9ms, page=100 1306.6→161.1ms.
각 서버는 별도 JVM이므로 엄밀히 동일한 실행 조건의 부하 시험은 아닙니다.
네 요청의 전체 data가 동일했고, 수정 서버는 임시 500ms 기준을 통과했습니다.

DB 실행 계획에서 자동완성의 전체 행 스캔은 약 1106ms였으며,
기존 제목 인덱스에서 후보 ID 6개를 먼저 제한하고 PK로 조인하면 약 148ms였습니다.
기존 `idx_games_release_title`, `idx_games_release_title_rating` 인덱스가 있는 환경의 결과입니다.
후자는 `monitoring/search-offset-index.sql`로 별도 적용하는 기존 인덱스이며,
인덱스가 없는 DB에서는 같은 성능을 보장하지 않습니다. 이번 변경에서 DB 스키마는 수정하지 않았습니다.
부분 일치 검색의 인덱스 전체 스캔과 정렬, 전체 목록 COUNT 및 큰 OFFSET 비용은 남습니다.

## 필터 및 정렬 비교

`GameFilteredQueryRepository`는 선택한 필터만 SQL에 포함하고, 해당 정렬 값과 ID로
페이지를 먼저 선택합니다. 사용자 통계는 선택한 지표 하나를 게임별로 집계해 LEFT JOIN합니다.
COUNT에는 통계 JOIN을 포함하지 않습니다. 필터는 EXISTS를 사용해 다중 선택 시 중복 행을 방지합니다.

필터 조회에서는 `monitoring/search-filter-index.sql`을 앱 배포 전에 한 번 적용합니다.
`SHOW INDEX FROM games`로 중복 여부를 먼저 확인하세요. 2026-10-19 로컬 DB에는 적용했습니다.
ID로 출시일/제목/평점을 읽는 커버링 인덱스이므로 저장 공간과 데이터 갱신 비용이 추가됩니다.
측정 환경에서 옵티마이저가 계속 본문 PK 조회를 선택해, 필터가 있는 쿼리와 COUNT에만
`JOIN_INDEX(g idx_games_catalog_lookup)` 힌트를 사용합니다. MySQL 8.4 실행 계획으로 검증했습니다.
힌트의 동작은 [MySQL 공식 문서](https://dev.mysql.com/doc/refman/8.4/en/optimizer-hints.html)를 참고하세요.
기존 자동완성과 필터 없는 기본 목록 경로는 유지합니다.

```powershell
$cases = @(
    'page?page=0&size=15&genreIds=4',
    'page?page=0&size=15&platformIds=1',
    'page?page=0&size=15&genreIds=4&platformIds=1&keyword=a',
    'page?page=0&size=15&sort=LATEST',
    'page?page=0&size=15&sort=TITLE',
    'page?page=0&size=15&sort=RATING',
    'page?page=0&size=15&sort=LIBRARY',
    'page?page=0&size=15&sort=PLAY_TIME'
)
# 필터 ID는 /api/v1/games/filters 응답에서 확인합니다. 아래 값은 로컬 DB 기준입니다.
./k6/measure-search.ps1 -Cases $cases -Samples 3 -OutputPath k6/results/filters-before.json
# 인덱스를 적용하고 수정 빌드로 재시작한 후 실행합니다.
./k6/measure-search.ps1 -Cases $cases -Samples 3 -OutputPath k6/results/filters-after.json -ComparePath k6/results/filters-before.json
```

로컬 284,294게임, 각 1회 예열/3회 측정 중앙값(ms): 장르 2058.4→125.0,
플랫폼 2089.9→366.5, 검색어+두 필터 2669.7→181.9, 최신순 1016.8→115.5,
제목순 1062.4→126.9, 평점순 1196.2→151.1, 등록 수순 1233.6→146.7,
플레이 시간순 1231.8→144.0. 기존 8080과 수정 8081 서버를 순차 비교했으며
두 서버는 별도 JVM입니다. 개선 전은 새 인덱스 적용 전 측정입니다.
8개 요청의 전체 data와 다중 필터+검색어+5개 정렬의 page=0/100 결과가 전후 동일했습니다.

**한계:** 이 로컬 DB의 user_games/reviews는 0건입니다. 사용자 기록이 있는 데이터의
정렬 정확성은 H2 통합 테스트로 확인했지만, 실제 대량 기록에서의 집계 성능은 별도 측정이 필요합니다.
이 결과는 단일 사용자 순차 요청이며 동시 부하 성능을 뜻하지 않습니다.
선택 지표의 전체 기록 집계, 큰 OFFSET, 정확한 전체 개수 계산 비용은 남습니다.

## k6 부하 테스트

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
