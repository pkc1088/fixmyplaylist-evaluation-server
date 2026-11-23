### 결정 기록 목록

- `0001-initial-load-retry-without-outbox.md`: 참고 사례 초기 적재 부분 실패 시 Outbox를 두지 않고 동일 엔드포인트 재호출로 복구한다.
- `0002-promotion-outbox-and-async-sync.md`: 참고 사례 승격 시 MySQL과 Qdrant 정합성을 위해 Outbox와 재처리 구조를 채택한다.
- `0003-dead-status-permanent-exclusion.md`: `DEAD` 상태는 자동 평가에서 영구 제외하고 재평가 및 역전이 기능을 구현하지 않는다.
- `0004-dlq-manual-inspection-only.md`: DLQ 메시지는 자동 재소비 및 재처리하지 않고 기록 후 수동 확인과 재발행으로 운영한다.
- `0005-promotion-reference-id.md`: `ReferenceCase` ID를 원본 `EvaluationCase` ID와 동일하게 사용해 승격 및 색인 재시도의 1:1 멱등성을 보장한다.