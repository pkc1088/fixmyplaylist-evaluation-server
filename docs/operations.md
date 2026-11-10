# 운영

세부 운영 값(배포 명령, 인프라 설정, 환경 변수 이름)은 개발자가 직접 관리한다.

## 빌드·테스트
- `./gradlew build`, `./gradlew test`(태그별은 `-Dtags=<태그>`).
- 에이전트는 Codex 샌드박스(`src-only` 프로파일)에서 위 명령을 직접 실행한다. 
- 쓰기는 `src`, `docs`, `build`, `.gradle`, `~/.gradle`에만, 네트워크는 `services.gradle.org`, `plugins.gradle.org`, `repo.maven.apache.org`에만 열려 있다. 
- 다른 도메인에서 받아야 하는 의존성이나 Docker가 필요한 테스트는 샌드박스에서 실패한다. 이때는 실패 원인을 보고하고 사용자가 실행한다.

## 설정
- 설정 키(`app.kafka.*`, `app.csv.*`, `gemini.*`, `qdrant.*`, `evaluation.secret`)는 코드의 `@Value`에서 확인한다.
- 스키마는 `ddl-auto=none`이다. 개발자가 Cloud SQL에 수동으로 만들고 바꾼다. 에이전트는 DDL을 실행하지 않는다.

## 배포
- GitHub Actions의 `deploy.yml`이 Cloud Run으로 배포하고 GitHub Secrets 값을 주입한다.

## 관리자 API 사용 순서
모두 `POST /api/admin/rag/...`이고 `X-Evaluation-Secret` 헤더가 필요하다.
1. 처음 구축: `setup/initial`을 먼저 한다.
2. 임베딩 모델·차원을 바꾼 뒤, 또는 Qdrant가 MySQL과 어긋났을 때: `setup/reindex`.
3. 평가: `evaluate/pending`이 신규 수신과 밀린 건 평가를 한 번에 한다.

## 장애 시 확인 순서
1. **403**: 시크릿 헤더와 경로(`/api/admin/rag/` 아래인지)를 본다.
2. **초기 적재 중 실패**: 같은 호출을 다시 한다. Qdrant만 비었다고 의심되면 `setup/reindex`. CSV 오류는 응답 본문과 로그를 본다(오류여도 HTTP 200일 수 있다. F-001).
3. **평가가 안 됨**: 로그의 `Poll returned empty`, `Fatal Error During Manual Consumption`으로 Kafka 쪽(토픽 설정, 연결·인증)을 먼저 본다.
4. **계속 `FAILED`**: Gemini 응답·타임아웃, Qdrant 연결, 임베딩 호출 순으로 본다. `DEAD`는 사람이 검수한다.
5. **DLQ가 쌓임**: `Move To DLQ` 로그로 원인을 보고, 해결한 뒤 필요하면 수동 재발행한다. `CRITICAL: Fail to send DLQ message`가 보이면 원문이 그 로그에 있으니 DLQ 토픽 연결을 먼저 복구한다.
6. **근거 없이 판정됨**: Qdrant에 참고 사례가 색인되어 있는지 보고, 비었으면 `setup/reindex`.