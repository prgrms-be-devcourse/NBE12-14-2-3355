# 게임 설명 한국어 번역

IntelliJ 서버 실행 설정의 Environment variables에 `DEEPL_API_KEY`를 추가합니다. 키는 커밋하지 않습니다.
일반 서버 실행과 테스트에서는 키가 필요하지 않고 DeepL 요청도 발생하지 않습니다.

## 기존 DB 번역

local 프로필의 Program arguments:

```
--game.translation.enabled=true --game.translation.max-games=10
```

처음 10개를 검토한 다음 `max-games=495` 등으로 늘립니다. 이 값은 이번 실행의 최대 신규 번역 수입니다.
전체 게임 수 제한이 아니므로 나중에 추가된 게임도 같은 작업으로 처리됩니다.
IGDB 재수집도 함께 하려면 `--igdb.import.enabled=true`를 추가합니다. 수집 후 번역 Runner가 실행됩니다.
완료 후 번역 옵션을 제거합니다. DevTools 재시작도 새로운 실행이므로 옵션을 남겨두면 다음 미완료 게임을 처리합니다.

## 저장과 비용

- 번역은 `description`에 직접 저장합니다. 별도의 한국어 컬럼은 만들지 않습니다.
- `description_source`는 번역 전 원문 백업이자 최신 IGDB 원문입니다. 원문을 받으면 갱신됩니다.
- `description_translated_hash`는 성공적으로 번역한 원문의 SHA-256입니다. 같은 원문은 재번역하지 않습니다.
- 기존 영어 설명은 처음 처리할 때 source에 복사하고 커밋한 다음 API에 전송합니다.
- DeepL의 실제 계정 잔여 한도를 각 요청 전에 확인합니다. 이것은 무료 여부를 판단하는 기능은 아닙니다. 유료 계정은 과금될 수 있습니다.
- 실패하면 그 실행은 중단되고 기존 설명과 이미 완료된 번역은 유지됩니다. 다음 실행에서 미완료 건부터 재개합니다.
- 네트워크 실패 후 자동 재시도하지 않습니다. DeepL에서 처리한 뒤 응답이 유실되면 다음 실행에서 중복 과금 가능성이 있습니다.
- 한 번에 한 서버에서 실행하세요. 여러 서버의 동시 번역에 대한 분산 잠금은 없습니다.
- 이름/장르/플랫폼/시리즈는 번역하지 않습니다.

local의 ddl-auto=update는 신규 컬럼을 추가합니다. 기존 DB/운영에서는 먼저 다음 스키마 변경을 적용합니다(이미 있는 컬럼은 다시 추가하지 않습니다).

```sql
ALTER TABLE games ADD COLUMN description_source TEXT NULL,
                  ADD COLUMN description_translated_hash VARCHAR(64) NULL;
```

검증:

```sql
SELECT id, title, description, description_source FROM games
WHERE description_translated_hash IS NOT NULL LIMIT 10;
SELECT COUNT(*) AS translated FROM games WHERE description_translated_hash IS NOT NULL;
```

API 문서: https://developers.deepl.com/api-reference/translate/request-translation
