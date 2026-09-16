# GameLog 게임 탐색

Next.js App Router / React / TypeScript 기반 게임 검색 화면입니다.

## 실행

```sh
cd front
npm ci
npm run dev
```

브라우저에서 http://localhost:3000 을 엽니다. 백엔드도 실행해야 실제 게임이 표시됩니다.
기본 백엔드 주소는 `http://localhost:8080`입니다. 변경하려면 `.env.example`을 `.env.local`로 복사하고 `BACKEND_URL`을 수정한 뒤 프론트 서버를 재시작하세요.

## 기능

- 검색창에서 Enter 또는 검색 버튼으로 제목 검색
- 장르와 플랫폼 다중 선택 후 필터 적용 (같은 분류는 OR, 서로 다른 분류는 AND)
- 기본순 / 최신 출시일순 / 제목순 정렬
- 페이지당 12개의 커버 카드 및 페이지 이동
- 검색·필터·정렬 변경 시 첫 페이지로 이동
- 커버 클릭 시 상세 정보 대화상자 (Escape로 닫기)
- 모바일 필터 펼치기, 로딩·빈 결과·연결 오류·누락 이미지 처리
- 하단의 디자인 미리보기 버튼: 명시적인 샘플 데이터 모드. 실제 결과와 혼합하지 않습니다.

## API

브라우저는 Next.js `/api/games/*` 경로를 호출하며 Next.js 서버가 Spring API로 GET 요청을 전달합니다.

- `/api/v1/games/page`: keyword, genreIds, platformIds, sort, page, size
- `/api/v1/games/filters`: DB의 실제 장르·플랫폼 ID와 이름. 이번 작업에서 추가했으므로 백엔드 재시작이 필요합니다.
- `/api/v1/games/{id}`: 카드 상세 정보

API 목록 응답에는 현재 플랫폼 정보가 없으므로 실제 모드의 플랫폼은 상세 대화상자에서 확인할 수 있습니다. 로그인, 라이브러리, 자동완성은 이 화면의 구현 범위에 포함되지 않습니다.
미리보기의 출시일은 샘플이며 실제 IGDB 데이터가 아닙니다. 샘플 커버는 Steam CDN을 사용합니다.

## 검증

```sh
npm run lint
npm run build
```
