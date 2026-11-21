### 프로젝트 개요

- 이 프로젝트는 FixMyPlaylist의 YouTube 음원 영상 복구 내역 이벤트를 수신해, 그 결과의 정확도를 RAG + LLM-as-a-Judge로 평가하는 이벤트 기반 마이크로서비스이다.
- AI 판정을 통해 개발자의 사후 평가 업무 부담을 줄이기 위함이 목적이다.
- 일 1회 배치성으로 실행되며, 일 평가 규모는 100건 이하이다.


### 네비게이션 트리

````
project-root/
├── AGENTS.md                     ← agent entry point
└── docs/
    ├── architecture.md           ← system composition
    ├── business-rules.md         ← domain logic
    ├── security.md               ← security policy
    ├── standards.md              ← rules (hard gates, conventions)
    ├── engineering-notes.md      ← knowledge (traps, mechanisms, checklists)
    ├── operations.md             ← operations (setup, build, deploy)
    ├── contracts.md              ← external interface contracts
    └── tracking/
        ├── status.md             ← current position vs full scope
        ├── decisions/
        │   ├── index.md          ← ADR index
        │   └── NNNN-<slug>.md    ← individual decisions
        └── findings.md           ← unresolved problems
````


### 어떤 작업에 어떤 문서를 읽나

- `docs/architecture.md`: 시스템 구성, 계층, 의존 방향, 외부 시스템 의존성
- `docs/business-rules.md`: 도메인 용어 정리, 도메인 로직, 상태 전이, 계산 로직, 엣지 케이스의 명시적 처리 방식
- `docs/security.md`: 인증 메커니즘, 자격 증명 관리, 시크릿, 로그·외부 전송 경계
- `docs/standards.md`: 코드 규칙, 모듈 경계 규칙, 환경 설정 규칙, 테스트 규칙
- `docs/engineering-notes.md`: 함정이나 디버깅 지식, 알려진 한계, 의도된 단순화
- `docs/operations.md`: 개발/빌드/검증 명령, 설정/배포/장애 시 확인 순서
- `docs/contracts.md`: 엔드포인트, 외부 API의 상태 코드/에러 포맷 매핑, Kafka 이벤트 페이로드, DB 스키마, Qdrant 및 CSV 형식
- `docs/tracking/status.md`: 프로젝트의 전체 범위 대비 현재 위치, 완료 및 남은 기능 기록
- `docs/tracking/decisions/`: 실제 트레이드오프가 있는 결정만 기록
- `docs/tracking/findings.md`: 즉시 해결되지 않는 미해결 문제만 기록


### 절대 규칙

- Git 저장소 상태나 이력을 변경하는 명령(add, commit, push, reset, checkout, switch, amend, merge, rebase, cherry-pick, restore, clean 등)을 실행하지 않는다. 변경은 파일로만 남긴다.
- 사용자가 지정한 Git Worktree 안에서만 작업한다. 반영 여부는 사용자가 리뷰 후 결정한다. 지시 범위 밖의 파일은 수정하지 않는다.
- 비밀 값이나 자격 증명은 코드, 문서, 로그, 작업 보고에 기록하지 않는다.
- 질문은 되돌리기 어렵거나 설계/범위가 바뀌는 모호함에만 한다. 사소한 것은 합리적으로 가정하고 진행한 뒤 가정을 보고한다.
- 코드를 변경하면 같은 작업에서 관련 `docs/`와 `docs/tracking/`을 갱신한다. 세부 검증 및 기록 기준은 `docs/standards.md`를 따른다.