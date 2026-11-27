### 사전 요구 사항

- 단위 빌드와 단위 테스트: JDK 17만 필요하다. `application.yml`, Docker, 외부 인프라 없이 실행한다.
- 통합 테스트와 로컬 기동: JDK 17, 로컬 `application.yml`, Docker 또는 실제 연동 인프라가 필요하다.
- 운영 인프라 관리 경계: Cloud Run, Cloud SQL, Qdrant Cloud, Confluent Cloud Kafka 토픽, GitHub Actions 배포 설정은 운영자가 직접 관리한다.


### 빌드 및 검증 명령

- 기본 검증 명령: `./gradlew test -Dtags=unit`
- 단위 테스트 포함 전체 빌드: `./gradlew build -Dtags=unit`
- 실행 JAR 패키징: `./gradlew bootJar` 실행 시 `build/libs/app.jar`가 생성된다.
- 컨테이너 통합 테스트: 로컬 `application.yml`과 Docker 컨테이너 인프라가 준비된 환경에서만 `./gradlew test -Dtags=integration`을 실행한다.
- 통합 테스트 컴파일 검증: Docker를 사용할 수 없는 샌드박스 환경에서 통합 테스트 코드를 수정한 경우 테스트 실행 대신 `./gradlew compileTestJava`로 컴파일만 검증한다.
- 실행 제약: `application.yml`이나 Docker가 없는 환경에서는 `-Dtags=integration`과 태그를 생략한 `./gradlew test`, `./gradlew build`를 실행하지 않는다. `build.gradle`은 `tags` 프로퍼티가 없으면 통합 테스트까지 모두 실행한다.
- 에이전트 환경별 Gradle 실행 분기: Codex처럼 기본 `~/.gradle` 쓰기와 네트워크가 허용된 환경에서는 위 명령을 그대로 실행한다. Antigravity처럼 `$HOME/.gradle-sandbox`가 마운트되어 있고 외부 네트워크가 차단된 샌드박스에서는 모든 `./gradlew` 명령 앞에 `GRADLE_USER_HOME="$HOME/.gradle-sandbox"`를 붙이고 뒤에 `--no-daemon --offline` 옵션을 추가한다.
    - 예시: `GRADLE_USER_HOME="$HOME/.gradle-sandbox" ./gradlew test -Dtags=unit --no-daemon --offline`
- 캐시 무시 재실행: `build` 디렉터리가 개별 마운트된 샌드박스에서는 `./gradlew clean`을 실행하지 않는다. 테스트를 강제로 다시 실행해야 할 때는 `--rerun` 옵션을 붙여 실행한다.


### 환경 설정

- 로컬 설정 파일인 `application.yml`은 `.gitignore` 대상이며 저장소에 포함하지 않는다. 세부 키 이름은 코드의 `@Value`와 설정 바인딩을 기준으로 확인한다.
- `evaluation.secret`: 관리자 API 인증용 정적 시크릿이다. 필수이며 기본값이 없어 누락 시 애플리케이션 기동이 실패한다.
- `spring.datasource.*`: Cloud SQL MySQL 접속 URL과 계정 정보이다.
- `spring.jpa.hibernate.ddl-auto`: 운영 환경은 반드시 `none`으로 유지하며 스키마 변경은 운영자가 수동 DDL로 적용한다.
- `app.kafka.*`: Confluent Cloud Kafka 접속 정보, 입력 토픽, DLQ 토픽, 수동 풀링 컨슈머 설정이다.
- `gemini.*`: Gemini API 키, 임베딩과 판정 모델, 온도 등 LLM 호출 설정이다.
- `qdrant.*`: Qdrant Cloud 접속 정보, API 키, 컬렉션 이름이다.
- `app.csv.*`: 초기 적재용 참고 사례 CSV 위치 설정이다. `app.csv.bucket`을 지정하면 GCS에서 읽고, 비워 두면 클래스패스 CSV를 읽는다.
- GCS 인증: 별도 키 설정을 두지 않으며 런타임 기본 자격 증명을 사용한다.


### 배포 및 데이터 초기화 순서

- 배포 경로: GitHub Actions의 `.github/workflows/deploy.yml`이 Cloud Run에 배포하고 GitHub Secrets를 런타임 환경 변수로 주입한다.
- 초기 구축 순서: Cloud SQL에 `evaluation_cases`와 `reference_cases` 테이블을 먼저 생성한 뒤 Cloud Run에 배포하고, `setup/initial`로 참고 사례를 적재한 다음 `evaluate/pending`을 실행한다. `setup/initial` 전에 평가를 실행하면 Qdrant 색인이 비어 있어 참고 사례 근거 없이 판정된다.
- 스키마 변경 반영 순서: 운영 Cloud SQL에 수동 DDL을 먼저 적용한 뒤 애플리케이션을 배포한다. 배포를 먼저 하면 신규 테이블이나 컬럼 참조 시 런타임 SQL 오류가 발생한다.
- 임베딩 변경 및 색인 복구 순서: 임베딩 모델, 벡터 차원, 입력 텍스트 형식을 변경해 배포했거나 Qdrant 색인이 MySQL 원본과 어긋났을 때는 `setup/reindex`를 실행해 Qdrant 포인트 수와 Cloud SQL `reference_cases` 행 수를 맞춘다.
- 초기 적재 부분 실패 복구: `setup/initial` 실행 중 실패하면 동일 엔드포인트를 재호출한다.
- 승격 색인 실패 복구: `POST /api/admin/rag/promotion/indexing/retry`를 호출한다. 요청 기반 Cloud Run은 인스턴스가 0일 때 백그라운드 스케줄러가 실행되지 않으므로 이 API를 운영 재시도 진입점으로 사용한다. Qdrant 포인트 ID가 결정적이어서 재색인은 같은 포인트를 덮어쓴다.