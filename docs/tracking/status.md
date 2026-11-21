### 완료된 것

- 관리자 API 인증 필터: `/api/admin/rag/**` 경로 차단, `X-Evaluation-Secret` 헤더 검증, `evaluation.secret` 미설정 시 기동 실패. 구현 완료, 자동 테스트 미검증.
- Kafka 수동 풀링 및 Inbox 적재: 입력 토픽 수동 풀링, `eventId` 기준 중복 무시 저장, 검증 및 저장 실패 시 원문 DLQ 발행. 구현 완료, 자동 테스트 미검증.
- RAG 및 LLM 평가 실행: `POST /api/admin/rag/evaluate/pending` 기준 신규 및 이전 실패 건 합산 순회, 건별 격리 실행, Gemini 임베딩, Qdrant 상위 5건 검색, Gemini 판정, 최대 3회 재시도와 `DEAD` 전이. 구현 완료, 자동 테스트 미검증.
- 참고 사례 초기 적재 및 재색인: `POST /api/admin/rag/setup/initial`의 GCS 또는 클래스패스 CSV 적재와 `POST /api/admin/rag/setup/reindex`의 MySQL 기반 Qdrant 전체 재색인. 구현 완료, 자동 테스트 미검증.


### 남은 것

- 도메인, 서비스, 어댑터 단위 테스트 작성 및 `./gradlew test -Dtags=unit` 검증 통과.
- 컨테이너 기반 통합 테스트 작성 및 로컬 Docker 환경에서 `./gradlew test -Dtags=integration` 검증 통과.
- 승격 시 `ReferenceCase` ID 생성 규칙 확정 및 `docs/tracking/decisions/` 기록.
- 사람 검수 기능 구현: `QueryEvaluationCasePort.loadPendingReview` 호출 경로 연결, 검수 대기 목록 조회 및 `AI_EVALUATED`에서 `HUMAN_REVIEWED`로의 상태 전이 API 구현.
- 참고 사례 수동 승격 기능 정식 구현: `docs/tracking/decisions/0002-*` 기준 MySQL 반영과 Outbox 기록 단일 트랜잭션 처리, 비동기 재처리를 통한 Gemini 임베딩 및 Qdrant 색인 동기화, 기존 `ReferenceCasePromotionService` 초안 대체.


### 블로킹

- 참고 사례 수동 승격 기능 구현: 승격 시 `ReferenceCase` ID 생성 규칙이 확정되지 않아 착수가 보류되어 있다. 세부 내용은 `docs/tracking/findings.md` 참조.