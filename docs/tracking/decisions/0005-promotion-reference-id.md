### 배경

- `EvaluationCase`를 승격할 때 `ReferenceCase`의 식별자와 재요청 멱등 기준을 결정해야 한다.
- Qdrant 포인트 ID는 `ReferenceCase` ID에서 결정적으로 생성되므로 키 정책이 색인 정합성에도 영향을 준다.


### 결정

- `ReferenceCase.referenceCaseId`는 원본 `EvaluationCase.evaluationCaseId`와 같은 값으로 저장한다.
- 한 평가 사례는 최대 하나의 참고 사례로 승격한다. 재요청은 같은 검수 라벨/사유에 한해 색인 복구 목적으로 허용하고, 다른 검수값 변경은 거부한다.


### 검토한 대안

- 별도 ID를 발급하고 `evaluationCaseId`를 유니크 컬럼으로 추가: 현재 도메인은 단일 승격이며, 추가 컬럼과 키 매핑이 지금 필요하지 않아 기각했다.


### 결과 및 제약

- EvaluationCase와 ReferenceCase의 키가 같아 MySQL 및 결정적 Qdrant 포인트를 쉽게 대조할 수 있다.
- 한 EvaluationCase로부터 복수 참고 사례를 만들려면 별도 ID 도입과 데이터 마이그레이션이 필요하다.