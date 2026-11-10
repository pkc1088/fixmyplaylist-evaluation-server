# 코딩 규칙과 완료의 정의

아래는 현재 코드에서 일관되게 지켜지는 규칙이다. 새 코드도 같은 방식을 따른다.

## 코드 규칙
- 도메인 모델은 정적 팩토리(`create`/`createPending`/`reconstitute`)로만 만들고, 상태는 setter가 아니라 의도가 드러나는 메서드(`handleFailure`, `applyAiEvaluation` 등)로만 바꾼다. 도메인에 JPA·Jackson 애노테이션을 달지 않는다.
- JPA 엔티티는 `Persistable`로 삽입/갱신을 직접 정한다. 도메인과 엔티티 변환은 `*Mapper`에서만 한다. 삽입은 `save`, 갱신은 `update` 포트 메서드를 쓴다.
- 이름: 유스케이스 `*UseCase`, 포트 `Command*Port`/`Query*Port`, 구현 `*Adapter`/`*JpaAdapter`. 의존성은 생성자 주입이다.
- 엔티티를 바꾸면 필요한 DDL을 보고에 적는다(스키마는 수동 관리).

## 트랜잭션
- `saveToInboxIdempotent`는 트랜잭션 없이 호출한다. 중복 키 예외를 잡아 무시하는 방식이라, 바깥 트랜잭션이 있으면 깨진다.
- MySQL과 Qdrant를 한 트랜잭션처럼 다루지 않는다.

## 에러 처리 방침
- Kafka 레코드 한 건, 평가 한 건의 실패가 나머지를 막지 않는다. 건별로 잡아 상태나 DLQ에 남기고 계속 진행한다.
- 예외를 삼키는 곳에서는 건 식별자와 예외를 로그에 남긴다.

## 테스트
- 테스트 코드는 아직 없다. 단위와 통합은 JUnit 태그로 나눠 `-Dtags`로 실행할 계획이다. 태그 이름은 "unit", "integration"으로 구분한다.
- 테스트는 외부 서비스(Gemini, Qdrant, Kafka, Cloud SQL, GCS) 실서버를 호출하지 않는다. 가짜나 로컬 대체물로 바꾼다.
- 가짜는 `port.out` 경계에서 만든다. domain은 mock 없이, service는 포트를 Mockito로 대체해 Spring 컨텍스트 없이 테스트한다.
- Spring Boot 3.4에서 `@MockBean`은 deprecated이므로 `@MockitoBean`을 쓴다.
- Gemini 응답은 고정 JSON으로 대체한다. LLM이 어떤 판정을 내리는지가 아니라, 응답과 실패를 우리 코드가 처리하는 방식이 테스트 대상이다.
- Qdrant는 HTTP가 아니라 WireMock으로 대체할 수 없다. `EmbeddingStore`를 가짜로 대체한다.
- 실제 인프라가 필요한 통합 테스트는 통합 태그로 분리한다. 에이전트 샌드박스는 Docker를 열어 두지 않았으므로 컨테이너가 필요한 테스트는 사용자가 실행한다.

## 완료의 정의
빌드·테스트 명령의 실제 종료 결과를 보고해야 완료다. 실행하지 못했으면 이유와 함께 그렇게 보고하고, 통과했을 것이라고 추정하지 않는다. 가정한 것은 보고에 적는다.

## 코드를 바꿀 때 같이 갱신할 문서
| 바꾼 것 | 문서 |
|---|---|
| 계층·의존 방향 | `docs/architecture.md` |
| 동작 규칙(상태, 승격, DLQ 등) | `docs/business-rules.md` |
| 필터, 비밀 값, 외부 전송 | `docs/security.md` |
| 이벤트·프롬프트·CSV·DB 형식 | `docs/contracts.md` |
| 설정, 배포 | `docs/operations.md` |
| 새로 알게 된 함정·한계 | `docs/engineering-notes.md` |
| 진행 상황, 이슈, 확정된 결정 | `docs/tracking/` |