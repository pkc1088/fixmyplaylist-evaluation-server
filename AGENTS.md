# AGENTS.md — 평가 서버 (FixMyPlaylist Evaluation Service)

FixMyPlaylist의 YouTube 복구 결과(원본 영상 → 대체 영상)가 적절한지를 RAG + LLM-as-a-Judge(Gemini)로 평가하는 이벤트 기반 마이크로서비스다. AI 판정은 사람이 검수할 대상을 좁히기 위한 것이며, 참고 사례(ReferenceCase)로 올리는(승진) 최종 결정은 항상 개발자가 한다.

## 기술 스택 (build.gradle에서 확인된 것)
- Java 17(toolchain), Gradle, Spring Boot 3.4.4 (web, data-jpa, validation, aop)
- LangChain4j 1.11.9(core, google-ai-gemini)와 langchain4j-qdrant 1.18.1-beta28. 모듈 버전이 서로 달라 경고가 뜨지만 문제없는 상태다. 지시 없이 맞추지 않는다.
- spring-kafka, spring-retry, opencsv 5.9, google-cloud-storage(libraries-bom 26.86.0), mysql-socket-factory-connector-j-8 1.14.1, Lombok
- 저장소와 인프라: MySQL(Cloud SQL), Qdrant(벡터 DB), Kafka(Confluent Cloud), Google Cloud Run
- 산출물: `bootJar` → `build/libs/app.jar`

## 검증 명령어
- 빌드: ./gradlew build
- 테스트: ./gradlew test, 태그별은 ./gradlew test -Dtags=<태그>
- 현재 테스트 코드는 없다(추후 도입 예정).

## 어떤 작업에 어떤 문서를 읽나
- 평가 흐름, 계층, 의존 방향, 외부 의존성: `docs/architecture.md`
- 상태 전이, 검수·승격, DEAD, DLQ 등 동작 규칙: `docs/business-rules.md`
- 관리자 API, 시크릿, 로그·외부 전송 경계: `docs/security.md`
- 코딩 컨벤션, 에러 처리, 테스트, 완료의 정의, 문서 갱신: `docs/standards.md`
- 이상해 보이는 코드, 알려진 한계, 의도된 단순화: `docs/engineering-notes.md`
- 설정, 배포, 장애 시 확인 순서: `docs/operations.md`
- 엔드포인트, Kafka 메시지, Gemini/Qdrant/CSV 형식, DB 스키마: `docs/contracts.md`
- 진행 상황, 발견 사항, 결정 기록: `docs/tracking/`

## 절대 규칙
- git 이력을 바꾸는 명령(commit, push, reset, checkout, amend 등)을 실행하지 않는다. 변경은 파일로만 남긴다.
- 사용자가 지정한 Git Worktree 안에서만 작업한다. 반영 여부는 사용자가 리뷰 후 결정한다. 지시 범위 밖의 파일은 수정하지 않는다.
- 질문은 되돌리기 어렵거나 설계/범위가 바뀌는 모호함에만 한다. 사소한 것은 합리적으로 가정하고 진행한 뒤 가정을 보고한다.
- 코드를 바꾸면 같은 작업에서 관련 docs도 갱신한다. 작업 중 발견했지만 지금 고치지 않는 문제는 `docs/tracking/findings.md`에 적는다.
- 비밀 값(API 키, 시크릿, 비밀번호)을 코드, 문서, 로그, 작업 보고에 쓰지 않는다. 설정 키 이름만 쓴다.
- EvaluationCase → ReferenceCase 승격(promotion)은 개발자가 승인한 수동 작업이다. 자동 승격 로직(예: confidence 1.0이면 자동 편입)을 만들지 않는다.
- 승격 기능은 아직 미구현이며 구현할 때는 Outbox와 재처리 구조로 만든다(`docs/tracking/decisions/0002-*.md`). `ReferenceCasePromotionService`는 Outbox가 없는 초안이므로 그대로 호출 경로에 연결하지 않는다.
- 참고 사례의 원본은 MySQL이고 Qdrant는 검색용 색인이다. 색인을 직접 고쳐서 MySQL과 어긋나게 만들지 않는다.
- 헥사고날 의존 방향을 지킨다(`docs/architecture.md`).
- 관리자 엔드포인트는 반드시 `/api/admin/rag/` 아래에 둔다. 이 경로만 시크릿 필터가 막아 준다.
- DB 스키마는 `ddl-auto=none`으로 사용자가 Cloud SQL에 수동 적용한다. 스키마 변경이 필요한 코드를 바꾸면 필요한 DDL을 작업 보고에 적고, 적용은 하지 않는다.
- 테스트에서 외부 서비스(Gemini, Qdrant, Kafka, Cloud SQL, GCS) 실서버를 호출하지 않는다. 가짜나 로컬 대체물로 바꿔서 테스트한다. 테스트와 설정 예시에 실제 키·시크릿을 넣지 않는다.

## 건드리면 안 되는 영역 (지시가 있을 때만 변경)
- Gemini 프롬프트와 모델·온도 설정(`GeminiEvaluatorAdapter`, `GeminiConfig`)
- 임베딩 모델과 차원(`GeminiConfig`), `RAG_TOP_K`. 임베딩 모델이나 차원을 바꾸면 Qdrant 전체 재색인이 필요하다.
- `EvaluationSecretFilter`의 검사 로직. 약화하거나 우회하지 않는다.
- `EvaluationCaseInboxService.saveToInboxIdempotent`의 트랜잭션 경계(호출 쪽에서 트랜잭션을 열면 중복 이벤트 처리가 깨진다).
- 배포 설정(GitHub Actions `deploy.yml`)과 운영 인프라(Cloud Run, Cloud SQL, Qdrant, Kafka 토픽). 사용자가 직접 관리한다.
- 상태·라벨 enum의 이름(`EvaluationStatus`, `EvaluationLabel`). DB에 문자열로 저장되어 있다.
- `build.gradle`, `settings.gradle`, `gradlew`, `gradle/`은 샌드박스에서 읽기 전용이다. 의존성이나 빌드 설정 변경이 필요하면 수정하지 말고 제안만 한다.