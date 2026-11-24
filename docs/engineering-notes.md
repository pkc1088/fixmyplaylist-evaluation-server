### 구현 및 프레임워크 함정

- `saveToInboxIdempotent` 외부 트랜잭션 중첩
    - 증상: 호출 측에 `@Transactional`을 붙이면 중복 이벤트 수신 시 예외를 잡아도 트랜잭션이 롤백 전용으로 표시되어 `UnexpectedRollbackException`이 발생한다.
    - 원인: 중복 차단이 DB 기본 키 충돌 예외를 잡아 무시하는 방식이므로 바깥 트랜잭션이 열려 있으면 예외 발생 시점에 트랜잭션 전체가 오염된다.
    - 대응: `saveToInboxIdempotent` 호출 경로 상위에 트랜잭션을 열지 않는다.

- `Persistable` 엔티티의 삽입과 갱신 혼용
    - 증상: 애플리케이션이 ID를 직접 할당하는 엔티티에서 `isNew` 판단이 어긋나거나 갱신 경로에 삽입 메서드를 혼용하면 불필요한 선행 `SELECT`와 병합이 발생하거나 기본 키 충돌이 난다.
    - 원인: Spring Data JPA는 ID가 null이 아니면 기존 엔티티로 판단해 `merge`를 실행한다.
    - 대응: `Persistable` 구현을 유지하고 신규 저장은 `save`, 기존 수정은 `update` 경로로 분리해 사용한다.

- Qdrant 테스트의 WireMock 대체 불가
    - 증상: WireMock으로 Qdrant 통신을 모킹하면 연결이 실패하거나 요청이 잡히지 않는다.
    - 원인: `langchain4j-qdrant` 클라이언트는 HTTP가 아니라 gRPC 프로토콜을 사용한다.
    - 대응: 테스트에서 HTTP 모킹 대신 `EmbeddingStore`를 가짜 구현체나 Mockito로 대체한다.

- LangChain4j 모듈 간 버전 불일치 경고
    - 증상: 의존성 해석이나 빌드 시 `langchain4j` 코어 계열과 `langchain4j-qdrant` 모듈 버전 차이로 경고가 출력된다.
    - 원인: `langchain4j-qdrant` 베타 아티팩트의 배포 버전 체계가 코어 모듈과 다르다.
    - 대응: 런타임 동작에 문제가 없으므로 경고 제거를 목적으로 버전을 임의 정렬하지 않는다.

- `EvaluationStatus` 및 `EvaluationLabel` enum 이름 변경
    - 증상: enum 상수 이름을 바꾸면 기존 DB 레코드 조회 시 매핑 예외로 역직렬화가 실패한다.
    - 원인: Cloud SQL에 enum 순서 번호가 아니라 문자열 이름 그대로 저장된다.
    - 대응: 기존 상수 이름을 변경하지 않는다. 변경이 불가피하면 기존 데이터 마이그레이션 DDL을 먼저 준비한다.

- Promotion Outbox와 재처리
  - 증상: MySQL 승격 저장과 Qdrant 색인간 부분 실패 가능성이 있다.
  - 원인: MySQL 승격 저장과 Qdrant 색인을 한 트랜잭션으로 묶을 수 없다.
  - 대응: MySQL 트랜잭션은 EvaluationCase, ReferenceCase, Outbox만 기록하고 커밋 후 Qdrant 색인을 수행한다. 색인 실패는 멱등한 재시도로 복구한다.


### 코드만으로 알기 어려운 동작과 의도된 단순화

- Qdrant 포인트 ID의 결정적 UUID 생성
    - 원리: Qdrant 포인트 ID를 `referenceCaseId`에서 유도한 결정적 UUID로 만든다.
    - 영향: 초기 적재나 재색인을 여러 번 실행해도 동일 사례가 중복 생성되지 않고 덮어쓰인다. 무작위 UUID로 바꾸면 재호출 복구 전략이 깨진다.

- 초기 적재의 Outbox 미적용
    - 원인: 관리자 수동 트리거 작업이므로 부분 실패 시 동일 엔드포인트 재호출로 복구하도록 단순화했다.
    - 대응: 초기 적재 경로에 Outbox나 별도 분산 보상 로직을 임의로 추가하지 않는다.

- 평가 호출의 건수 상한과 타임아웃 가드 미적용
    - 원인: 일 1회 호출, 일 100건 이하 규모, 관리자 의도적 실행을 전제하므로 응답 시간 최적화나 대량 분할 처리를 두지 않았다.
    - 대응: 규모 전제가 바뀌기 전까지 대량 처리용 복잡도를 추가하지 않는다.

- 유사 사례 검색의 유사도 하한 미적용
    - 원인: Qdrant 검색 시 점수 하한 없이 상위 `RAG_TOP_K`건을 그대로 반환한다.
    - 대응: 별도 지시 없이 임의의 유사도 임계값을 추가해 결과를 걸러내지 않는다.

- 프롬프트와 모델 파라미터 튜닝 금지
    - 원인: 과거 PoC 수치는 운영 변경 판단 기준이 아니다.
    - 대응: `GeminiEvaluatorAdapter`와 `GeminiConfig`의 프롬프트, 모델, 온도, `RAG_TOP_K`는 명시적 지시가 있을 때만 수정한다.


### 반복 작업 체크리스트

- 임베딩 모델, 벡터 차원, 임베딩 입력 텍스트 형식 변경 시
    - 기존 Qdrant 벡터 공간과 달라져 검색 품질이 깨지거나 차원 불일치 오류가 발생한다.
    - `docs/contracts.md`의 임베딩 사양과 `docs/security.md`의 외부 전송 범위를 함께 갱신한다.
    - 배포 후 Qdrant 전체 재색인이 필요하다는 점을 작업 보고에 명시한다. 실제 재색인 실행과 건수 대조 절차는 `docs/operations.md`를 따른다.

- JPA 엔티티 스키마 변경 시
    - `ddl-auto=none` 설정이므로 코드만 배포하면 운영 환경에서 SQL 오류가 발생한다.
    - `src/test/resources/schema.sql`을 변경된 엔티티 구조에 맞게 갱신하고, 운영 Cloud SQL 반영에 필요한 MySQL DDL을 작업 보고에 포함한다.
    - 단위 테스트로 `*Mapper` 양방향 변환과 `Persistable`의 `isNew` 분기 동작이 유지되는지 확인한다.
