# FixMyPlaylist - Evaluation Service

**FixMyPlaylist의 복구 정확도를 RAG & LLM-as-a-Judge로 자동 평가하는 서버리스·이벤트 기반 마이크로서비스**

---
## Overview

- FixMyPlaylist 복구 작업의 품질 검수 자동화를 목표(Human-in-the-loop)
- 개발자의 검수 업무 부하 최소화를 위해 LLM-as-a-Judge 도입
- 기존 Zero-shot 프롬프팅의 한계 극복을 위한 RAG 파이프라인 설계
- Human Verification 된 유사 검증 사례를 검색하여 현재 복구 결과의 적합성 판단에 활용 
- PoC 검증을 통해 검수 정확도 45% 향상 및 수동 검수 물량 39% 자동화 가능성 확보

---
## Architecture & Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.4.4
- **AI Framework**: LangChain4j
- **Database**: MySQL 8.0
- **Vector Database:** Qdrant
- **Persistence**: Spring Data JPA
- **LLM**: Gemini 2.5 Flash Lite
- **Embedding Model**: Gemini Embedding 001
- **Infra**: Cloud SQL, Confluent Cloud, Qdrant Cloud, [In Progress]
- **Message**: Apache Kafka
- **Monitoring**: [In Progress]

---
## Architecture Design

### 1. RAG 기반 자동 평가 파이프라인 구축
**Context**:
- 복구 정확도 평가를 위해 사람이 모든 복구 결과를 직접 검수해야 하므로, 복구 건수가 증가할수록 평가 비용이 증가.

**Decision**:
- Human Labeling 된 실제 운영 데이터를 Reference Dataset으로 구축하고, 현재 복구 결과와 유사한 과거 사례를 RAG로 검색하여 LLM-as-a-Judge의 판단 근거로 제공.
- 동일한 Test Dataset에 Zero-shot과 RAG 방식을 적용하여 RAG의 실질적인 효용성을 검증.

**Result**:
- 100건의 PoC 테스트에서 정확도 26%p 향상(Zero-shot: 58% → RAG: 84%).
- Zero-shot 오답 중 RAG에서 정답으로 개선된 사례 26건 확인.
- Zero-shot 정답이 RAG에서 오답으로 변경된 사례 0건 확인.


### 2. Confidence 기반 Human-in-the-loop
**Context**:
- LLM의 모든 평가 결과를 개발자가 다시 검수하면 AI 평가를 도입한 의미가 감소.
- 반대로 AI 판단을 그대로 신뢰하면 오판 가능성 존재.

**Decision**:
- LLM의 판단 결과에 Confidence를 추가하여 Human Review 대상을 선별.
- PoC에서 검증한 초기 기준으로 Confidence 1.0은 자동 승인하고, 그 외 결과는 Human Review 대상으로 분류.
- Reference Dataset은 Human Verification이 완료된 데이터만 편입하여 검증된 판단 근거로 유지.

**Result**:
- Confidence 1.0인 39건 중 정답 39건, 오답 0건 확인.
- 수동 검수 물량의 39%를 자동화하여 실질적인 운영 리소스 대폭 절감 가능성 확인.

---