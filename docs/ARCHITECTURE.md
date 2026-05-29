---
level: 2
mode: committed
status: active
last_verified: 2026-05-29
confidence: 0.85
owners: [박세현]
description: 패키지 구조·기술스택·데이터 흐름·레이어 규칙 — 패키지 추가/이동·API 변경·기술스택 변경 시 반드시 갱신.
---

# ARCHITECTURE — 시스템 아키텍처 (L2)

---

## 1. 패키지 구조

```
src/main/java/com/dku/opensource/be/
├── api/              # REST API 컨트롤러
│   ├── AuthController.java
│   ├── BillController.java
│   ├── FeedController.java
│   ├── LegislationController.java
│   ├── PetitionController.java
│   ├── TempDataController.java
│   └── UserProfileController.java
├── batch/            # Spring Batch 공공데이터 수집 파이프라인
│   ├── job/          # Job 정의 (Bill, Legislation, Petition × Collect·Embedding)
│   ├── step/
│   │   ├── dto/      # 공공 API 응답 DTO
│   │   ├── processor/ # ItemProcessor
│   │   ├── reader/   # ItemReader (공공 API 호출 전담)
│   │   └── writer/   # ItemWriter (DB 저장)
│   └── scheduler/    # BatchScheduler (매일 자정)
├── domain/           # Entity + Repository (JPA)
│   ├── auth/         # User
│   ├── bill/         # Bill
│   ├── legislation/  # LegislationNotice
│   ├── petition/     # Petition
│   └── user/         # UserProfile
├── recommendation/   # 임베딩 모델 + pgvector 코사인 유사도 추천
├── agent/            # ReAct 에이전트 (미구현 — KAN-12)
│   ├── react/        # ReAct 루프
│   ├── tool/         # Agent Tool 정의
│   └── model/        # EXAONE 3.5 HTTP 호출
├── notification/     # FCM 푸시 알림 (미구현 — KAN-11)
├── security/         # JwtFilter, JwtUtil, SecurityConfig
└── common/           # ApiResponse, GlobalExceptionHandler
```

---

## 2. 기술 스택

| 범주 | 기술 |
|---|---|
| 언어·런타임 | Java 17, Spring Boot 4.x, Gradle |
| DB | PostgreSQL + pgvector 확장 (코사인 유사도 검색) |
| 마이그레이션 | Flyway (V1~V9) |
| 배치 | Spring Batch + Spring Scheduler |
| 알림 | Firebase Cloud Messaging (FCM) |
| 추천 임베딩 | 한국어 경량 모델 (ko-sroberta 류) |
| 에이전트 LLM | EXAONE 3.5 (LG AI, HTTP 호출) |
| 보안 | JWT (JwtFilter) |

---

## 3. 레이어 규칙

| 규칙 | 내용 |
|---|---|
| Entity 위치 | 반드시 `domain/` 패키지 |
| API 응답 | `ApiResponse<T>` 공통 래퍼 |
| 공공 API 호출 | `batch/step/reader/` ItemReader에서만 — 서비스 레이어 직접 호출 금지 |
| pgvector 쿼리 | Native Query (`@Query(nativeQuery = true)`) |
| LLM 호출 | `agent/model/` 에서만 |
| ReAct 루프 | `agent/react/` 에서만 |
| Agent Tool | `agent/tool/` 에 위치 |
| 임베딩·EXAONE 역할 | 추천 = 임베딩 모델, 에이전트 = EXAONE — 혼용 금지 |

---

## 4. 데이터 흐름

```
[공공 API] → Spring Batch (ItemReader) → ItemProcessor → ItemWriter → PostgreSQL
                                                                           ↓
                                                              임베딩 Batch Job
                                                                           ↓
                                                              pgvector (벡터 컬럼)
                                                                           ↓
[iOS 클라이언트] → REST API → RecommendationService → pgvector 코사인 유사도 → 피드 응답
                           → AgentController (KAN-12) → ReAct 루프 → EXAONE → 답변
                           → FCM Scheduler (KAN-11) → D-7/D-3/D-1 알림
```

---

## 5. DB 마이그레이션 현황 (Flyway)

| 파일 | 내용 |
|---|---|
| V1__init.sql | pgvector 확장 초기화 |
| V2__create_tables.sql | 주요 테이블 생성 |
| V3__alter_legislation_notice.sql | legislation_notice 수정 |
| V4__add_user_embedding.sql | user 임베딩 컬럼 추가 |
| V5__add_bill_committee.sql | bill 위원회 컬럼 추가 |
| V6__add_user_age_occupation.sql | user 나이·직업 컬럼 추가 |
| V7__create_users.sql | users 테이블 생성 |
| V8__add_petition_embedding.sql | petition 임베딩 컬럼 추가 |
| V9__add_legislation_embedding.sql | legislation 임베딩 컬럼 추가 |
