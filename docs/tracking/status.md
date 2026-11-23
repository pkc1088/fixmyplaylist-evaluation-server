### 완료된 것

- 관리자 API 인증 필터: `/api/admin/rag/**` 경로 차단, `X-Evaluation-Secret` 헤더 검증, `evaluation.secret` 미설정 시 기동 실패. 구현 완료, 자동 테스트 미검증.
- Kafka 수동 풀링 및 Inbox 적재: 입력 토픽 수동 풀링, `eventId` 기준 중복 무시 저장, 검증 및 저장 실패 시 원문 DLQ 발행. 구현 완료, 자동 테스트 미검증.
- RAG 및 LLM 평가 실행: `POST /api/admin/rag/evaluate/pending` 기준 신규 및 이전 실패 건 합산 순회, 건별 격리 실행, Gemini 임베딩, Qdrant 상위 5건 검색, Gemini 판정, 최대 3회 재시도와 `DEAD` 전이. 구현 완료, 자동 테스트 미검증.
- 참고 사례 초기 적재 및 재색인: `POST /api/admin/rag/setup/initial`의 GCS 또는 클래스패스 CSV 적재와 `POST /api/admin/rag/setup/reindex`의 MySQL 기반 Qdrant 전체 재색인. 구현 완료, 자동 테스트 미검증.
- 사람 검수/승격: `/admin-review.html` UI, 인증된 검수 대기 목록 GET, `human_label`/`human_reason` POST, EvaluationCase 상태 전이, ReferenceCase 저장, Outbox 기반 Qdrant 색인과 관리자 재시도 경로를 구현. Java 컴파일 통과, 자동 테스트 미실행.


### 남은 것

- 도메인, 서비스, 어댑터 단위 테스트 작성 및 `./gradlew test -Dtags=unit` 검증 통과.
- 컨테이너 기반 통합 테스트 작성 및 로컬 Docker 환경에서 `./gradlew test -Dtags=integration` 검증 통과.