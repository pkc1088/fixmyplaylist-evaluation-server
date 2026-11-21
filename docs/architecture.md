### 시스템 토폴로지

- 일 1회 Cloud Scheduler가 HTTP 요청으로 요청 기반 min-instance 0 Cloud Run 인스턴스를 호출한다.
- 상시 실행 Kafka 컨슈머를 두지 않으며, 호출 시점에만 Kafka 수동 풀링과 평가를 실행하고 HTTP 응답 후 종료한다.
- 외부 연동 대상은 Confluent Cloud Kafka, Cloud SQL MySQL, Qdrant Cloud, Gemini API, GCS이다.


### 모듈 맵과 의존 방향

- 기준 패키지는 `evaluation.evaluationService.evaluation`이다.
- `adapter.in`: HTTP 진입점과 응답 변환. `port.in` 의존.
- `port.in`: 유스케이스 진입 인터페이스. `domain` 참조.
- `application.service`: 평가 순회와 유스케이스 조율. `port.in` 구현, `port.out`과 `domain` 의존.
- `port.out`: 외부 연동 추상화. `domain` 참조.
- `adapter.out`: DB, Kafka, Qdrant, Gemini, GCS 구현체. `port.out` 구현.
- `domain`: 평가와 참고 사례 모델. JDK와 Lombok 외 의존 없음.
- `infrastructure.config`: 외부 클라이언트와 어댑터 빈 조립, 공통 필터 등록.
- 경계: 어댑터 간 직접 호출을 금지한다. `jakarta.persistence`, Kafka, LangChain4j, Qdrant, GCS 타입은 `adapter.out`과 `infrastructure.config` 밖으로 넘어가지 않는다.


### 대표 실행 흐름

- 평가 실행: Cloud Scheduler HTTP 요청 → Kafka 입력 토픽 수동 풀링 → Cloud SQL `evaluation_cases` 신규 적재 → 신규 건과 이전 실패 건 합산 순회 → 건별 격리 실행으로 Gemini 임베딩 생성 → Qdrant 유사 사례 검색 → Gemini 판정 → `evaluation_cases` 결과 반영 → HTTP 응답 반환.
- 평가 예외 분기: 처리 불가 메시지는 Kafka DLQ 토픽으로 발행한다. 단건 평가 실패는 나머지 건의 순회를 중단시키지 않는다.
- 초기 적재: 관리자 HTTP 요청 → GCS CSV 읽기 → Cloud SQL `reference_cases` 저장 → Gemini 임베딩 → Qdrant 색인.
- 재색인: 관리자 HTTP 요청 → Cloud SQL `reference_cases` 전체 조회 → Gemini 임베딩 → Qdrant 전체 재색인.
- 승격: 관리자 HTTP 요청 → Cloud SQL `reference_cases` 반영과 Outbox 기록을 단일 트랜잭션으로 처리 → 별도 재처리 경로에서 Gemini 임베딩과 Qdrant 색인 비동기 동기화.


### 외부 의존성과 경계

- Confluent Cloud Kafka: 입력 토픽 수동 풀링과 DLQ 발행만 수행한다. 원본 토픽 발행 권한과 상시 리스너는 없다.
- Cloud SQL MySQL: `evaluation_cases`, `reference_cases` 테이블을 소유하며 참고 사례의 원본 저장소이다. 타 서비스 테이블에 접근하지 않는다.
- Qdrant Cloud: `reference_cases`의 파생 벡터 색인과 유사도 검색 전용이다. 원본 저장소가 아니며 MySQL을 거치지 않는 직접 수정을 금지한다.
- Gemini API: HTTPS로 임베딩과 판정을 호출한다. 영상 제목과 사람 검수 사유가 외부로 전송되며, 세부 전송 제한은 `docs/security.md`를 따른다.
- GCS: 초기 적재용 참고 사례 CSV 읽기 전용이다. 버킷 쓰기나 수정은 하지 않는다.