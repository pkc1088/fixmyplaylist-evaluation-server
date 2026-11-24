### 도메인 용어

- `EvaluationCase`: 수신된 복구 이벤트 한 건의 평가 기록으로, 원본 이벤트 정보, AI 판정 결과, 사람 검수 결과를 함께 보관한다.
- `ReferenceCase`: 사람이 검증 완료해 RAG 판정 근거로 사용하는 사례이다.
- `confidence`: LLM이 `EvaluationCase`를 `SUCCESS`/`FAIL`로 평가할 때, 얼만큼 확신하는지를 표현하는 0~1.0의 실수값.
- 복구(`Recovery`): FixMyPlaylist에서 비정상 음원의 원본 영상인 `target`을 대체 음원 영상인 `source`로 교체하는 작업이다.
- 승격(`Promotion`): `EvaluationCase`를 검증된 `ReferenceCase`로 편입하는 작업이다.
- 자동 승인: 시스템 기능이 아니라 개발자가 DB에서 `confidence` 1.0이고 `SUCCESS`인 건을 골라 검수를 간소화하는 운영 방식을 가리킨다. 이 표현을 근거로 상태를 자동 변경하는 로직을 구현하지 않는다.


### 판정 위임 규칙

- 복구 결과의 `SUCCESS`와 `FAIL` 판정은 검색된 `ReferenceCase`와 외부 LLM 평가에 위임한다.
- 애플리케이션 도메인 로직에 별도의 `SUCCESS` 또는 `FAIL` 판정 기준을 하드코딩하거나 추측해 추가하지 않는다.


### EvaluationCase 상태 전이

- 신규 `eventId`가 수신되면 `PENDING` 상태로 생성된다.
- `PENDING` 또는 `FAILED` 상태에서 AI 평가가 성공하면 `AI_EVALUATED`로 전이한다.
- `PENDING` 상태에서 첫 평가가 실패하면 `FAILED`로 전이하고 시도 횟수를 1로 기록한다.
- `FAILED` 상태에서 재평가가 실패하고 누적 시도 횟수가 3회 미만이면 `FAILED` 상태를 유지한다.
- `FAILED` 상태에서 3번째 평가 시도가 실패하면 `DEAD`로 전이한다.
- `AI_EVALUATED` 상태에서 개발자 검수가 완료되면 `HUMAN_REVIEWED`로 전이한다.
- `AI_EVALUATED`, `HUMAN_REVIEWED`, `DEAD` 상태의 건은 자동 평가 대상으로 다시 실행하지 않는다.
- 같은 요청 재전송은 동일한 라벨과 사유일 때만 멱등 재시도로 취급한다. 이미 검수된 건에 다른 값을 보내면 충돌로 거부한다.
- `DEAD` 상태의 건은 자동 평가에서 영구 제외해 사람이 직접 검수하며, `PENDING`이나 `FAILED`로 되돌리는 역전이를 허용하지 않는다.


### 이벤트 수신과 엣지 케이스

- 동일한 `eventId`는 한 번만 저장한다. 이미 저장된 `eventId`가 다시 수신되면 새 레코드를 생성하거나 기존 상태를 덮어쓰지 않고 무시한다.
- DB 일시 장애를 포함해 수신 처리나 저장에 실패한 이벤트는 원문을 변형하거나 유실하지 않고 그대로 DLQ로 보낸다.
- DLQ 메시지는 기록과 수동 확인 대상이며, 확인 후 필요 시 사람이 수동 재발행한다. DLQ를 자동으로 재소비하거나 재처리하지 않는다.


### 검수와 승격 불변식

- `ReferenceCase`에는 사람이 검증을 마친 사례만 등록한다.
- `EvaluationCase`에서 `ReferenceCase`로의 승격은 개발자가 명시적으로 승인한 수동 작업으로만 실행한다.
- `confidence`가 1.0이거나 판정이 `SUCCESS`인 경우를 포함해 어떤 조건에서도 자동 승격을 수행하지 않는다.
- `ReferenceCase` ID는 원본 `EvaluationCase` ID를 그대로 사용해 1:1 관계와 재시도 멱등성을 보장한다.


### 참고 사례 초기 적재 규칙

- 초기 적재는 여러 번 반복 실행해도 동일한 상태를 유지하는 멱등 작업이어야 한다.