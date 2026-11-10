# 아키텍처

평가 서버는 복구 서버가 발행한 "복구 완료" 이벤트를 받아 저장하고, 사람이 검증한 과거 사례를 근거로 Gemini가 복구 결과를 판정하게 한 뒤, 그 결과를 DB에 남긴다. 
상시 실행되는 리스너 없이, 예정된 Cloud Scheduler가 호출하는 HTTP 엔드포인트가 한 번 실행될 때마다 처리가 일어나는 구조다. 평가는 1일 1회 수행된다.

## 패키지와 계층, 의존 방향 규칙
기준 패키지는 `evaluation.evaluationService.evaluation`이며 헥사고날(Ports & Adapters) 구조다.
- `adapter.in` → `port.in` ← `application.service` → `port.out` ← `adapter.out`. 어댑터끼리는 서로 부르지 않는다.
- `domain`은 아무것도 의존하지 않는다(Lombok과 JDK만 사용).
- LangChain4j, Kafka 클라이언트, JPA(`jakarta.persistence`), Qdrant, GCS 타입은 `adapter.out`과 `infrastructure.config` 안에서만 쓴다.
- 포트는 읽기/쓰기로 나눈 이름을 쓴다: `Command*Port`, `Query*Port`.
- 생성자 인자가 설정 값인 어댑터(`CsvReaderAdapter`, `KafkaMessagePullAdapter`, `KafkaDlqAdapter`)는 `@Component`가 아니라 `infrastructure.config`의 `@Bean`으로 만든다.
- 알려진 예외: `CsvPort`가 opencsv의 `CsvException`을 시그니처에 노출한다(`docs/tracking/findings.md`).

## 핵심 모델
- `EvaluationCase`: 들어온 이벤트 한 건의 평가 기록이다. 이벤트 수신(Inbox), AI 판정, 사람 검수 결과를 한 레코드에 담는다.
- `ReferenceCase`: 사람이 검증한 사례. RAG의 판정 근거로만 쓰인다.
- `EvaluationCase`의 상태: `PENDING` → `AI_EVALUATED` → `HUMAN_REVIEWED`, 평가 실패 시 `FAILED`(재시도 대상) → `DEAD`(재시도 종료). 전이 규칙은 `docs/business-rules.md`에 있다.

## 흐름에서 코드만으로 안 보이는 것
**평가**: 
- 호출 한 번이 "Kafka 신규 수신 → `PENDING` 저장 → `PENDING`·`FAILED` 전부 평가"를 모두 한다. 
- 신규 건과 이전 실패 건이 같은 순회에서 처리된다. 
- 평가와 결과 저장은 건별로 격리되어 한 건의 실패가 다른 건을 막지 않는다.
**초기 적재 / 재색인**: 
- MySQL이 원본이고 Qdrant는 색인이다. 초기 적재는 CSV → MySQL → Qdrant 순이고, 재색인은 MySQL 전체를 다시 Qdrant에 넣는다. 
- 중간에 실패하면 같은 엔드포인트를 다시 호출한다(`docs/tracking/decisions/0001-*` 결정).
**승격**: 
- 미구현이다. 구현할 때는 Outbox와 재처리 구조로 만든다(`docs/tracking/decisions/0002-*` 결정). 
- `QueryEvaluationCasePort.loadPendingReview()`(검수 대기 조회, confidence 낮은 순)도 같은 이유로 호출하는 곳이 없다.


## 외부 의존성과 경계
- Kafka(Confluent Cloud): 입력 토픽 읽기, DLQ 토픽 쓰기. 이벤트 발행자는 이 저장소 밖의 복구 서버다. 이 서비스는 읽기와 DLQ 쓰기만 한다.
- Cloud SQL(MySQL): `evaluation_cases`, `reference_cases`. 이 서비스가 두 테이블의 소유자다. 스키마는 수동 관리다.
- Qdrant Cloud: 참고 사례 벡터 색인·검색. 컬렉션 이름과 접속 정보는 설정으로 받는다. 색인은 MySQL에서 재생성 가능하다.
- Gemini API: 임베딩(`gemini-embedding-001`, 768차원)과 판정(`gemini-3.5-flash-lite`). 제목과 사람 사유가 외부로 전송된다(`docs/security.md`).
- GCS: CSV 참고 사례 파일 읽기. `app.csv.bucket`이 비어 있으면 클래스패스 파일을 읽는다. 인증은 기본 자격 증명을 쓴다.
