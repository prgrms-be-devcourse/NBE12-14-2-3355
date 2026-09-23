# IGDB 단독 수집

IntelliJ에서 기존 Railway 수집용 실행 설정을 복제하고 Main class를
`com.gamelog.igdbimport.IgdbImportApplication`으로 변경합니다.
Active profiles는 `local`로 두고 기존 IGDB 키와 DB 연결 환경변수를 유지합니다.

필수 환경변수:

```properties
IGDB_CLIENT_ID=<client id>
IGDB_CLIENT_SECRET=<client secret>
SPRING_DATASOURCE_URL=jdbc:mysql://<Railway 공개 호스트>:<공개 포트>/<DB 이름>
SPRING_DATASOURCE_USERNAME=<DB 사용자>
SPRING_DATASOURCE_PASSWORD=<DB 비밀번호>
```

터미널 환경변수에 위 값을 설정했다면 `back`에서 `./gradlew.bat importIgdb`로도 실행할 수 있습니다.
IntelliJ 실행 설정의 환경변수는 터미널로 자동 전달되지 않습니다.

이 진입점은 `IGDB_IMPORT_ENABLED` 값과 관계없이 한 페이지를 수집한 뒤 종료합니다.
현재 페이지 범위는 기존 `IgdbClient.fetchGames()`의 limit/offset을 사용합니다.
동일 페이지를 다시 실행하면 IGDB ID 기준으로 갱신합니다.
완료 로그의 처리 개수에는 갱신도 포함됩니다.

웹 서버, 로그인, 리뷰, 번역, Cloudinary 구성은 로드하지 않습니다.
게임 관련 7개 엔티티만 로드하므로 refresh_token/users/reviews 테이블은 필요 없습니다.
게임 테이블은 미리 존재해야 합니다. 실행 시 `ddl-auto=validate`, SQL 초기화 `never`를
명시해 운영 테이블을 자동 생성·변경하지 않습니다.
실패 시 비정상 종료하며, 성공 시 `IGDB 게임 수집 완료: ...개 처리` 로그 후 연결을 닫습니다.

기존 웹 앱의 기본 실행 및 배포 main class는 변경되지 않습니다.
