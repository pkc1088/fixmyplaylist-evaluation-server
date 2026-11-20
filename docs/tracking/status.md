구현만 마치고 검증 명령이나 테스트 통과를 확인하지 않은 기능은 status.md에 완료로 표기하지 않고 미검증 상태로 구분한다.

## 완료된 것

## 남은 것
- 승격 기능은 아직 미구현이며 구현할 때는 Outbox와 재처리 구조로 만든다(`docs/tracking/decisions/0002-*.md`). `ReferenceCasePromotionService`는 Outbox가 없는 초안이므로 그대로 호출 경로에 연결하지 않는다.



## 블로킹