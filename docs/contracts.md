# 계약

형식 자체는 코드가 말해 주므로, 바꾸면 다른 쪽이 깨지는 약속과 제약만 적는다.

## Kafka 입력 이벤트 (복구 서버 → 이 서비스)
- 값은 JSON이고 필드는 `eventId`, `targetTitle`, `sourceTitle`이다. 모르는 필드는 무시한다.
- `eventId`(복구 작업 UUID + ActionLog 번호)가 `EvaluationCase`의 ID이자 중복 방지 키다. 100자를 넘거나 제목이 공백이면 DLQ로 간다.
- 발행자는 저장소 밖의 복구 서버다. 필드나 의미를 바꾸려면 양쪽을 함께 바꾼다.
- DLQ에는 입력 원문 문자열을 가공 없이 보낸다.

## 임베딩과 Qdrant
- 임베딩 모델, 768차원, 텍스트 형식(`Target:` 줄 + `Source:` 줄)은 색인과 검색이 같아야 한다. 하나라도 바꾸면 전체 재색인이 필요하다.
- 점 ID는 `referenceCaseId`에서 만든 결정적 UUID이고, 검색은 필터·최소 점수 없이 상위 5건이다.

## Gemini 판정 응답
JSON 객체 `{ label: SUCCESS | FAIL, confidence: <실수> }`. 둘 다 필수이고, 프롬프트에서 confidence를 0.0~1.0으로 요청한다. 프롬프트에는 유사도 점수를 넣지 않는다.

## CSV 참고 사례 파일
UTF-8, 첫 줄은 헤더. 열 순서는 `id, source 제목, target 제목, 라벨(SUCCESS/FAIL), 사유`다. 열이 모자라거나 라벨이 틀리면 적재가 예외로 실패한다.

## DB 스키마 제약
스키마는 수동 관리이므로 실제 DB가 최종 기준이다.
- `evaluation_cases`의 기본 키는 `eventId`다. AI 판정·사람 검수 컬럼은 NULL 허용이고, 상태와 재시도 횟수는 NOT NULL이다. 
- `retrievedInfo`는 `{referenceCaseId, similarityScore}` 목록(JSON)이다.
- `reference_cases`는 사람 라벨과 사유가 NOT NULL이다.
- enum은 이름 문자열로 저장한다. `EvaluationStatus`·`EvaluationLabel`의 이름을 바꾸거나 지우면 기존 데이터가 깨진다.
- 시각은 타임존 없는 `LocalDateTime`이다.