### 의존성 및 빌드 파일 규칙

- `build.gradle`, `settings.gradle`, `gradlew`, `gradle/` 하위 파일은 직접 수정하지 않는다. 의존성 추가나 빌드 설정 변경이 필요하면 수정안만 제안한다.
- 외부 라이브러리 버전 변경이나 모듈 간 버전 정렬은 명시적 지시 없이 수행하지 않는다.


### 모듈 경계 및 임포트 규칙

- `adapter.in`은 `port.in`만 참조하며 `application.service`나 `adapter.out`을 직접 참조하지 않는다.
- `application.service`는 `port.in`을 구현하고 `port.out`과 `domain`에만 의존한다.
- `port.in` 반환 모델은 도메인 객체를 노출하지 않으며, 도메인에서 `port.in.dto`로의 변환은 `application.service`가 담당한다.
- `adapter.out`은 `port.out`을 구현하며 다른 어댑터를 직접 호출하지 않는다.
- `domain` 패키지는 JDK와 Lombok 외의 외부 라이브러리, JPA, Jackson 애노테이션을 임포트하지 않는다.
- `jakarta.persistence`, Kafka 클라이언트, LangChain4j, Qdrant, GCS 타입은 `adapter.out`과 `infrastructure.config` 외부로 노출하지 않는다.


### 네이밍 및 구조 규칙

- 클래스 접미사는 유스케이스 `*UseCase`, 쓰기 포트 `Command*Port`, 읽기 포트 `Query*Port`, 어댑터 `*Adapter` 또는 `*JpaAdapter`, 변환기 `*Mapper`를 사용한다.
- 모든 빈 의존성은 생성자 주입만 사용하며 필드 주입을 금지한다.
- 도메인 모델은 `create`, `createPending`, `reconstitute` 같은 정적 팩토리 메서드로만 생성한다. 공개 생성자와 setter를 두지 않으며 상태 변경은 의도가 드러나는 도메인 메서드로만 수행한다.
- 설정 값을 생성자 인자로 받는 어댑터는 `@Component`로 등록하지 않고 `infrastructure.config`의 `@Bean` 메서드로 등록한다.


### 내부 영속성 및 트랜잭션 경계 규칙

- 도메인 모델과 JPA 엔티티 간 변환은 `*Mapper`에서만 수행한다.
- JPA 엔티티는 `Persistable`을 구현해 신규 삽입과 갱신을 명시적으로 구분한다.
- 영속성 포트의 삽입 메서드는 `save`, 갱신 메서드는 `update`로 분리한다.
- MySQL 쓰기 트랜잭션 내부에서 Qdrant나 Gemini 같은 외부 네트워크 호출을 함께 묶어 단일 트랜잭션처럼 처리하지 않는다.


### 예외 로깅 규칙

- 건별 처리 중 예외를 잡아 흐름을 계속 진행하는 경우 반드시 해당 건의 식별자와 예외 객체를 함께 로그에 남긴다. 예외를 로그 없이 삼키지 않는다.


### 환경 설정 및 스키마 규칙

- `application.yml` 및 실제 환경 설정 파일은 `.gitignore` 대상이며 워크트리에 추가하거나 커밋 대상으로 생성하지 않는다.
- `evaluation.secret`을 포함한 인증 및 외부 연동 비밀 설정에 기본값을 두지 않는다.
- JPA 엔티티의 테이블이나 컬럼 구조를 변경하면 `src/test/resources/schema.sql`도 함께 갱신한다. Cloud SQL에 DDL을 직접 실행하지 않으며, 운영 반영에 필요한 DDL은 작업 보고에 포함한다.

### 테스트 규칙

- 워크트리에 `application.yml`이 존재하지 않으므로 자동 검증 테스트는 스프링 부트 컨텍스트를 기동하지 않는 순수 단위 테스트로 작성한다.
- `@SpringBootTest`처럼 애플리케이션 컨텍스트 기동이 필요한 테스트는 자동 검증 대상에 포함하지 않는다.
- 테스트 코드에서 Gemini, Qdrant, Kafka, Cloud SQL, GCS 실서버를 호출하지 않는다.
- `domain` 단위 테스트는 Mockito 없이 순수 자바 객체로 작성한다.
- `application.service` 단위 테스트는 `port.out` 인터페이스를 Mockito나 가짜 구현체로 대체한다.
- Gemini 연동 어댑터 테스트는 고정 JSON 응답으로 대체해 응답 파싱과 실패 분기 처리만 검증한다.
- Qdrant 연동 어댑터 테스트는 `EmbeddingStore`를 가짜 구현체나 Mockito로 대체한다.
- 스프링 컨텍스트 없이 실행하는 단위 테스트는 반드시 `@Tag("unit")`을 붙인다.
- 스프링 컨텍스트나 외부 컨테이너가 필요한 테스트에는 반드시 `@Tag("integration")`을 붙인다.


### 검증 및 문서 갱신 게이트

- 작업 완료 보고에는 실제 실행한 빌드 및 테스트 명령의 종료 결과를 포함한다. 명령을 실행하지 못한 경우 통과를 추정하지 않고 미실행 사유와 가정 사항을 함께 보고한다.
- `docs/tracking/status.md`에는 테스트 통과가 확인된 기능만 검증 완료로 표기하며, 구현만 마치고 테스트가 없거나 미실행된 기능은 미검증 상태로 구분해 기록한다.
- `docs/tracking/decisions/`에는 실질적인 대안과 트레이드오프가 존재하는 결정만 기록하며, 구체적인 기각 사유와 이후 작업에 생기는 제약을 포함한다.
- 작업 중 발견한 재현 가능한 결함은 즉시 수정하는 것을 원칙으로 한다. 외부 의존성이나 작업 범위 외 사유로 즉시 해결할 수 없는 경우에만 재현 조건, 영향 범위, 보류 사유를 `docs/tracking/findings.md`에 기록한다.