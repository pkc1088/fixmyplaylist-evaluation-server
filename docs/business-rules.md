### 용어
- 복구: 사용할 수 없게 된 원본 영상(`target`)을 대체 영상(`source`)으로 바꾸는 FixMyPlaylist의 작업.
- EvaluationCase: 복구 한 건의 평가 기록.
- ReferenceCase: 사람이 검증해 RAG 근거로 쓰는 사례.
- 승격: 전자를 후자로 올리는 일.
- `EvaluationCase`: 들어온 이벤트 한 건의 평가 기록이다. 이벤트 수신(Inbox), AI 판정, 사람 검수 결과를 한 레코드에 담는다.
- `ReferenceCase`: 사람이 검증한 사례. RAG의 판정 근거로만 쓰인다.
- `EvaluationCase`의 상태: `PENDING` → `AI_EVALUATED` → `HUMAN_REVIEWED`, 평가 실패 시 `FAILED`(재시도 대상) → `DEAD`(재시도 종료). 전이 규칙은 `docs/business-rules.md`에 있다.


### 판정 기준
- SUCCESS/FAIL 기준은 이 문서에 정의하지 않는다. 판정은 Gemini 프롬프트와 검색된 사람 검증 사례에 맡겨져 있고, 에이전트는 기준을 알 필요가 없다. 기준을 추측해 문서나 프롬프트에 쓰지 않는다.

### EvaluationCase 상태 전이
- 평가는 케이스당 최대 3번 시도한다. 3번째 실패에서 `DEAD`가 된다. 이미 AI 판정을 받은 건은 다시 평가하지 않는다.
- `DEAD`는 영구 제외하고 사람이 직접 검수한다. 되돌리는 기능은 만들지 않는다(`docs/tracking/decisions/0003-*`).

### 이벤트 수신 규칙
- 같은 `eventId`는 한 번만 저장한다(Kafka 중복 전달 대비).
- 처리에 실패한 이벤트는 원문 그대로 DLQ로 간다. DB 일시 장애로 못 저장한 이벤트도 포함한다.
- DLQ는 기록용이다. 사람이 확인하고 필요하면 수동으로 재발행한다. 자동 재처리는 없다(`docs/tracking/decisions/0004-*`).

### 검수와 승격
- AI 판정은 개발자가 수동 검수할 대상을 좁히기 위한 것이며, 평가 사례(EvaluationCase)를 참고 사례(ReferenceCase)로 승진시키는(Promotion) 최종 결정은 항상 개발자가 한다.
- EvaluationCase → ReferenceCase 승격(promotion)은 개발자가 승인한 수동 작업이다. 자동 승격 로직(예: confidence 1.0이면 자동 편입)을 만들지 않는다.
- 참고 사례에는 사람이 검증한 것만 들어간다.
- 승격은 개발자가 승인하는 수동 작업이다. 자동 승격은 없고 만들지 않는다.
- 승격과 검수 메뉴는 미구현이다. 구현할 때는 Outbox와 재처리 구조를 쓴다(`docs/tracking/decisions/0002-*`).
- "자동 승인"은 시스템 기능이 아니다. 개발자가 DB에서 confidence 1.0이면서 SUCCESS인 건을 골라 검수를 가볍게 넘기는 운영 방식을 가리키는 말이다. 이 말을 근거로 상태를 바꾸는 코드를 만들지 않는다.
- 승격 시 ReferenceCase ID를 EvaluationCase ID와 같게 쓸지는 [확인 필요: 승격 시 ID 규칙].

### 참고 사례 저장 규칙
- 초기 적재는 여러 번 실행해도 되는 작업이다. 같은 ID는 CSV 값으로 덮어쓰고, CSV에서 빠진 행은 지우지 않는다. 부분 실패하면 같은 엔드포인트를 다시 호출한다(`docs/tracking/decisions/0001-*`).
- 모든 임베딩은 같은 모델·차원·텍스트 형식으로 만든다. 하나라도 바꾸면 전체 재색인이 필요하다.
- Qdrant 색인을 직접 고쳐서 MySQL과 어긋나게 만들지 않는다.

### 관리자 호출 전제
- 평가·적재·재색인은 관리자가 의도적으로 부르는 작업이다. 응답 시간과 대량 처리는 설계상 신경 쓰지 않는다
