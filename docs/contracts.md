### HTTP 관리자 API 공통 규약

- 모든 엔드포인트는 `POST /api/admin/rag/` 하위 경로를 사용한다.
- 인증 헤더: 모든 요청에 `X-Evaluation-Secret` 헤더가 필수이다.
- 인증 오류: 헤더가 누락되었거나 설정값과 일치하지 않으면 HTTP 403을 반환한다. 실제 에러 응답 본문 형식은 필터 구현 기준으로 기입한다.
- 서버 오류: 처리 중 미처리 예외가 발생하면 HTTP 500을 반환한다.


### HTTP 관리자 API 목록

- `POST /api/admin/rag/setup/initial`
    - 입력: 요청 본문 없음.
    - 출력: HTTP 200과 초기 적재 처리 결과 본문을 반환한다. 실제 응답 필드 구조는 컨트롤러 기준으로 기입한다.
    - 오류: CSV 형식 오류나 부분 실패가 발생해도 HTTP 200으로 응답할 수 있으므로 응답 본문으로 실패 여부를 확인한다.
- `POST /api/admin/rag/setup/reindex`
    - 입력: 요청 본문 없음.
    - 출력: HTTP 200과 재색인 처리 결과 본문을 반환한다. 실제 응답 필드 구조는 컨트롤러 기준으로 기입한다.
    - 오류: MySQL 조회나 Qdrant·Gemini 연동 실패 시 오류 응답을 반환한다.
- `POST /api/admin/rag/evaluate/pending`
    - 입력: 요청 본문 없음.
    - 출력: HTTP 200과 평가 실행 결과 본문을 반환한다. 실제 응답 필드 구조는 컨트롤러 기준으로 기입한다.
    - 오류: Kafka 수동 풀링 치명 오류 등 실행 불가 상황에서 오류 응답을 반환한다. 개별 이벤트 검증 실패와 단건 평가 실패는 HTTP 요청을 실패시키지 않고 DLQ 발행이나 DB 상태 기록으로 처리한다.


### Kafka 이벤트 계약

- 발행 주체와 호환성: 입력 토픽은 외부 복구 서버가 발행한다. 기존 필드 이름이나 의미를 바꿀 때는 복구 서버와 이 서비스를 함께 변경한다.
- 입력 형식: JSON 객체로 `eventId`, `targetTitle`, `sourceTitle` 세 필드를 사용한다. 정의되지 않은 추가 필드는 무시한다.
- 입력 제약: `eventId`는 복구 작업 UUID와 ActionLog 번호 조합이며 최대 100자이다. `targetTitle`과 `sourceTitle`은 공백일 수 없다.
- 정상 출력: 유효한 신규 `eventId`는 `evaluation_cases`에 저장되며 이미 존재하는 `eventId`는 무시한다.
- 오류 출력: JSON 역직렬화 실패, `eventId` 100자 초과, 제목 공백, DB 저장 실패 시 입력 원문 문자열을 가공 없이 DLQ 토픽으로 발행한다.


### Gemini API 계약

- 임베딩 호출
    - 모델: `gemini-embedding-001`
    - 입력: `Target:` 줄과 `Source:` 줄로 구성된 고정 텍스트 형식이다. 색인과 검색 모두 동일한 형식을 사용한다.
    - 출력: 768차원 실수 벡터.
    - 오류: API 오류나 차원 불일치 시 해당 건의 평가 또는 적재 실패로 처리한다.
- 판정 호출
    - 모델: `gemini-3.5-flash-lite`
    - 입력: 평가 대상 제목과 검색된 참고 사례의 제목 및 사람 검수 사유. 유사도 점수는 프롬프트에 넣지 않는다.
    - 출력: `label`과 `confidence` 두 필드가 모두 필수인 JSON 객체이다. `label`은 `SUCCESS` 또는 `FAIL`, `confidence`는 0.0 이상 1.0 이하의 실수이다.
    - 오류: API 호출 실패, JSON 파싱 실패, 필수 필드 누락 시 단건 평가 실패로 처리한다.


### Qdrant 색인 및 검색 계약

- 포인트 ID: `referenceCaseId`에서 유도한 결정적 UUID를 사용한다.
- 벡터 사양: `gemini-embedding-001` 기반 768차원 벡터를 저장한다.
- 검색 입력과 출력: 필터와 최소 유사도 점수 없이 상위 5건을 반환한다.
- 오류: 연결 실패나 검색 오류 시 해당 건의 평가 또는 색인 실패로 처리한다.


### CSV 참고 사례 파일 계약

- 인코딩과 구조: UTF-8 인코딩이며 첫 줄은 헤더이다.
- 열 순서: `id`, `source` 제목, `target` 제목, 라벨, 사유 순이다. `source` 열이 `target` 열보다 앞에 온다.
- 필드 제약: 라벨 값은 `SUCCESS` 또는 `FAIL`만 허용한다.
- 오류: 열 개수가 부족하거나 허용되지 않은 라벨 값이 있으면 예외로 적재가 실패한다.


### Cloud SQL DB 스키마 계약

- `evaluation_cases`: 기본 키는 `eventId`이다. 상태와 재시도 횟수는 `NOT NULL`이며 AI 판정과 사람 검수 컬럼은 `NULL`을 허용한다.
- `retrievedInfo` 컬럼: `referenceCaseId`와 `similarityScore`를 필드로 갖는 JSON 배열로 저장한다.
- `reference_cases`: 사람 라벨과 사유 컬럼은 `NOT NULL`이다.
- enum 직렬화: 문자열 이름 그대로 저장한다. 상태 값은 `PENDING`, `AI_EVALUATED`, `HUMAN_REVIEWED`, `FAILED`, `DEAD`, 라벨 값은 `SUCCESS`, `FAIL`을 사용한다.
- 시각 컬럼: 타임존 오프셋 없는 `LocalDateTime` 형식으로 저장한다.