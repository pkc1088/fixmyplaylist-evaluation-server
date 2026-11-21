### 배경

- 개발자가 검수한 `EvaluationCase`를 `ReferenceCase`로 승격할 때는 Cloud SQL MySQL의 `reference_cases` 반영, Gemini 임베딩 생성, Qdrant 색인 추가가 모두 이루어져야 한다.
- 초기 적재와 달리 승격은 운영 중 검수 흐름 속에서 단건씩 반복 실행되는 상태 변경 작업이다.
- 기존 `ReferenceCasePromotionService` 초안처럼 단순 동기 호출과 강제 삽입으로 처리하면 MySQL 커밋 이후 외부 API 실패 시 정합성이 깨지고, 재시도 시 기본 키 충돌이 발생한다.


### 결정

- 참고 사례 승격 정식 구현은 Cloud SQL MySQL의 `reference_cases` 저장과 Outbox 이벤트 기록을 단일 DB 트랜잭션으로 묶는다.
- Gemini 임베딩 생성과 Qdrant 색인 반영은 별도 재처리 경로가 Outbox를 읽어 비동기 동기화하는 구조로 구현한다.


### 검토한 대안

- 초기 적재와 동일하게 동기 호출 후 실패 시 관리자 재호출: 승격은 검수 과정에서 반복 호출되므로 실패 건을 사람이 일일이 추적해 재실행해야 하며, 외부 API 응답 지연이 DB 트랜잭션 점유 시간을 늘리고 부분 실패가 누적될 위험이 커 기각했다.
- 승격 발생 시마다 `reference_cases` 전체 재색인 실행: 단건 추가마다 전체 참고 사례를 다시 임베딩하고 Qdrant에 적재하면 불필요한 Gemini API 비용과 지연이 발생해 기각했다.


### 결과 및 제약

- 승격 요청이 완료된 직후 Qdrant 검색 결과에 즉시 반영되는 강한 일관성은 보장하지 않으며 최종 일관성 모델로 동작한다.
- Outbox 테이블 스키마 추가, 수동 DDL 관리, 비동기 재처리 트리거 및 실패 재시도 상태 관리가 필수 선행 작업으로 뒤따른다.
- 기존 `ReferenceCasePromotionService` 초안은 그대로 호출 경로에 연결할 수 없으며 정식 구현 시 교체해야 한다.
- MySQL 쓰기 트랜잭션 내부에서 Gemini나 Qdrant 외부 네트워크 호출을 직접 묶는 구현은 허용하지 않는다.