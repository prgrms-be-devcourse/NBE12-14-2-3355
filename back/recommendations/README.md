# 연관 추천 수동 사전 계산

백엔드 서버 실행과 별도로 명령을 실행하며, 작업이 끝나면 종료한다. 일반 서버 시작만으로는 계산하지 않는다. 관리자 페이지와 자동 갱신은 후속 작업이다.

## 실행 준비

- Java 25, Docker MySQL과 1번 커밋에서 추가한 두 테이블이 필요하다.
- 명령은 `local` 프로필을 사용하되 스키마는 `validate`, SQL 초기화는 `never`로 실행한다. 테이블을 자동 생성하거나 초기 데이터를 실행하지 않는다.
- 접속 주소 기본값은 `application.yaml`의 local 설정이다. 현재 설정의 DB 이름은 `gamelog_retry`이므로, 실제 로컬 DB가 `gamelog`라면 아래처럼 연결 주소를 지정한다. 사용자·비밀번호는 기존 `DB_USERNAME`, `DB_PASSWORD` 환경변수를 사용하고 명령에 비밀번호를 직접 적지 않는다.

저장소 루트에서:

```bash
cd back
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/gamelog?serverTimezone=UTC&characterEncoding=UTF-8'
```

## 실행 명령

```bash
# 도움말: DB에 연결하지 않고 사용법 확인
./gradlew recommendationPrecompute --args='--help'

# 최초 계산 검증: 지정 게임 ID만 처리
./gradlew recommendationPrecompute --args='--game-ids=10,20,30 --mode=INITIAL_LOAD'

# 전체 최초 계산: 모든 기존 게임을 후보 모집단으로 사용
./gradlew recommendationPrecompute --args='--all --mode=INITIAL_LOAD'

# 일반 갱신: 준비 완료된 게임만 후보로 사용
./gradlew recommendationPrecompute --args='--all --mode=REFRESH'

# 실패한 ID만 재처리: 실패한 실행과 같은 모드 사용
./gradlew recommendationPrecompute --args='--game-ids=10,30 --mode=REFRESH'
```

`--all`과 `--game-ids` 중 하나 및 `--mode`를 반드시 지정한다. 지정 ID는 중복 제거 후 오름차순 처리한다. 전체 실행은 시작 시 최대 게임 ID를 고정하고 100개씩 조회하며, 실행 도중 추가된 게임은 다음 실행 대상으로 남긴다.

## 결과와 중복 실행

- 100건마다 진행 상황, 종료 시 처리·성공·실패 건수, 실패 ID, 소요 시간을 출력한다.
- 개별 게임 실패는 기존 정상 결과를 유지하고 다음 게임을 처리한다. 실패 ID는 같은 모드의 지정 ID 명령으로 재실행한다.
- 같은 MySQL 서버의 같은 DB에 대한 계산은 별도 터미널에서도 하나만 실행된다. DB별 이름의 `GET_LOCK`을 전용 연결에서 유지하고 종료 시 해제한다. 실제 연결이 종료되면 MySQL이 잠금을 해제하며, 잠금 연결 확인 실패 시 실행을 중단한다.
- 종료 코드: `0` 정상 완료, `1` 개별 실패 또는 실행 오류, `2` 다른 계산 실행 중. Gradle은 0이 아닌 종료 코드를 작업 실패로 표시한다.
- 추천 조회는 계속 가능하다. 이 명령은 로컬 사전 계산 테이블을 변경하지만, API의 사전 계산 조회 전환이나 전체 준비 완료 판정은 아직 수행하지 않는다.
