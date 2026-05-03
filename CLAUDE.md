# jigeumchamyeo-backend

Spring Boot 기반 백엔드 서버 + Spring Batch 공공데이터 수집 파이프라인.
한국 국회 의안정보 / 국민동의청원 / 입법예고 API를 수집하고, pgvector 기반 개인화 추천 피드 / ReAct 에이전트 기반 의안 Q&A / FCM 마감 임박 알림을 제공한다.

## Commands

```bash
# 빌드 및 실행
./gradlew bootRun

# 테스트
./gradlew test

# 특정 테스트만
./gradlew test --tests "com.dku.opensource.be.*"

# 빌드
./gradlew build

# Spring Batch 파이프라인 수동 실행
./gradlew bootRun --args='--spring.batch.job.name=billCollectJob'

# DB 마이그레이션 (Flyway)
./gradlew flywayMigrate
```

## Architecture

```
src/main/java/com/dku/opensource/be/
├── api/           # REST API 컨트롤러 (피드, 법안, 청원, 알림, 에이전트)
├── batch/         # Spring Batch 공공데이터 수집 파이프라인
│   ├── job/       # Job 정의
│   ├── step/      # Step (Reader / Processor / Writer)
│   └── scheduler/ # Spring Scheduler (매일 자정 실행)
├── domain/        # Entity, Repository (JPA)
├── recommendation/# 한국어 임베딩 모델 + pgvector 코사인 유사도 추천 로직
├── agent/         # ReAct 패턴 직접 구현 + EXAONE 3.5 의안 Q&A
│   ├── react/     # ReAct 루프 (Thought → Action → Observation)
│   ├── tool/      # Agent Tool 정의 (법안 검색, 상세 조회 등)
│   └── model/     # EXAONE 3.5 연동 (HTTP 호출)
├── notification/  # FCM 푸시 알림 (D-7 / D-3 / D-1)
└── common/        # 공통 유틸, 예외 처리, 설정
```

## Tech Stack

- Java 17, Spring Boot 4.x, Gradle
- PostgreSQL + pgvector 확장 (코사인 유사도 검색)
- Spring Batch (공공데이터 수집, 매일 자정)
- Spring Scheduler (마감 임박 알림 트리거)
- Firebase Cloud Messaging (FCM)
- Flyway (DB 마이그레이션)
- 한국어 임베딩 모델 (ko-sroberta 등 경량 모델) — 추천 피드용 벡터 생성
- EXAONE 3.5 (LG AI, 한국어 특화 LLM) — ReAct 에이전트 추론·답변 생성
- ReAct 패턴 직접 구현 — Thought / Action / Observation 루프

## Key Rules

- Entity는 반드시 `domain/` 패키지 안에 위치시킨다
- API 응답은 `ApiResponse<T>` 공통 래퍼를 사용한다
- 공공 API 호출은 `batch/step/` 안의 ItemReader에서만 수행한다 — 서비스 레이어에서 직접 호출하지 않는다
- pgvector 쿼리는 Native Query로 작성한다 (`@Query(nativeQuery = true)`)
- 환경변수(API 키, FCM 키 등)는 절대 코드에 하드코딩하지 않는다 — `application-secret.yml` 사용 (git 제외)
- Batch Job은 멱등성을 보장해야 한다 (중복 실행 시 데이터 중복 없음)
- 임베딩 모델(추천용)과 EXAONE(에이전트용)은 역할을 분리한다 — 추천에 EXAONE 사용 금지
- ReAct 루프는 `agent/react/` 에서만 구현한다
- LLM 모델 호출은 `agent/model/` 에서만 수행한다
- Agent Tool 정의는 `agent/tool/` 에 위치시킨다

## Gotchas

- pgvector 확장은 DB 초기화 시 `CREATE EXTENSION IF NOT EXISTS vector;` 필요 — Flyway 첫 마이그레이션에 포함되어 있음
- 공공 API는 일별 호출 한도가 있으므로 Batch에서 불필요한 중복 호출 금지
- FCM 토큰은 앱 재설치 시 변경되므로 `notification/` 에서 토큰 갱신 로직 확인 후 수정
- Spring Batch의 `JobRepository`는 PostgreSQL 스키마를 사용하므로 테스트 시 H2 대신 Testcontainers 사용

## 미완료 사항 (다음 세션 시 확인)

### 의안 본문(content) 미수집
- **현재 상태**: `TVBPMBILL11` API는 메타데이터(제목/발의자/위원회/상태)만 제공 — `content` 필드 없음
- **문제**: KAN-9 임베딩, KAN-12 에이전트 Q&A 모두 본문이 있어야 품질이 나옴
- **검토할 옵션**:
  1. `OK7XM1000938DS17215` API (발의법률안) 명세 확인 — 제안이유/주요내용 필드 존재 여부
  2. `LINK_URL` 크롤링 (JS 렌더링이라 Playwright 필요 — 배치에 붙이기 무거움)
  3. 일단 제목+위원회+발의자 기반으로 임베딩 진행, content는 추후 보완
- **권장**: 옵션 1 먼저 확인 후 없으면 옵션 3으로 우선 진행

### 청원 / 입법예고 API 키 미확보
- `application-secret.yml`의 `petition-api-key`, `legislation-api-key` 값이 `PLACEHOLDER`
- 해당 배치잡 실행 시 API 호출 실패 — 실제 키 발급 후 교체 필요
- 국민동의청원: https://petitions.assembly.go.kr (API 키 발급 경로 확인 필요)
- 입법예고: https://open.lawmaking.go.kr (API 키 발급 경로 확인 필요)

### 미구현 패키지
- `recommendation/` — KAN-9, 임베딩 + pgvector 코사인 유사도 추천
- `api/` — KAN-10, REST API 엔드포인트 전체
- `notification/` — KAN-11, FCM D-7/D-3/D-1 알림
- `agent/` — KAN-12, ReAct 루프 + EXAONE 3.5 + 의안 Q&A 툴

## Jira 작업 워크플로우

**Jira 보드**: https://dankook-opensource-project.atlassian.net/jira/software/projects/KAN/boards/1
**담당자**: parksehyn (dhaprk0429@dankook.ac.kr)
**Cloud ID**: `2a985da6-fb1b-48d0-8fee-8909b62d0ebb`

### 작업 진행 원칙

1. **세션 시작 시** — Jira에서 `project = KAN AND assignee = parksehyn AND status = "해야 할 일"` JQL로 미완료 작업 목록을 조회하고, 이슈 번호 오름차순으로 하나씩 순서대로 완수한다.
2. **작업 완수 후** — 해당 Jira 이슈를 `완료` 상태로 전환(transition)한다. 완료 전환을 빠뜨리지 않는다.
3. **모든 작업 완수 시** — 사용자에게 알리고, 다음에 등록할 새 작업을 함께 상의한 뒤 Jira에 신규 이슈로 등록하는 과정을 거친다. 임의로 새 작업을 만들지 않는다.

### 현재 작업 목록 (parksehyn 배정)

| 이슈 | 제목 | 상태 |
|------|------|------|
| [KAN-2](https://dankook-opensource-project.atlassian.net/browse/KAN-2) | 주제 선정 | ✅ 완료 |
| [KAN-3](https://dankook-opensource-project.atlassian.net/browse/KAN-3) | 서비스 흐름 및 기술 구현 방식 설정 | ✅ 완료 |
| [KAN-6](https://dankook-opensource-project.atlassian.net/browse/KAN-6) | [BE] 프로젝트 초기 세팅 (Spring Boot, Gradle, Flyway, PostgreSQL) | ✅ 완료 |
| [KAN-7](https://dankook-opensource-project.atlassian.net/browse/KAN-7) | [BE] 도메인 Entity 및 Repository 설계 (Bill, Petition, UserProfile) | ✅ 완료 |
| [KAN-8](https://dankook-opensource-project.atlassian.net/browse/KAN-8) | [BE] 공공데이터 수집 Spring Batch 파이프라인 구현 (국회 법안) | ✅ 완료 |
| [KAN-13](https://dankook-opensource-project.atlassian.net/browse/KAN-13) | [BE] 국민동의청원 수집 Spring Batch 파이프라인 구현 및 검증 | ✅ 완료 |
| [KAN-14](https://dankook-opensource-project.atlassian.net/browse/KAN-14) | [BE] 입법예고 수집 Spring Batch 파이프라인 구현 및 검증 | 해야 할 일 |
| [KAN-9](https://dankook-opensource-project.atlassian.net/browse/KAN-9) | [BE] pgvector 기반 개인화 추천 로직 구현 (코사인 유사도 + 마감일 필터) | 해야 할 일 |
| [KAN-10](https://dankook-opensource-project.atlassian.net/browse/KAN-10) | [BE] REST API 구현 (피드, 법안, 청원, 관심사 설정) | 해야 할 일 |
| [KAN-11](https://dankook-opensource-project.atlassian.net/browse/KAN-11) | [BE] FCM 마감 임박 푸시 알림 구현 (D-7 / D-3 / D-1) | 해야 할 일 |
| [KAN-12](https://dankook-opensource-project.atlassian.net/browse/KAN-12) | [BE] ReAct 에이전트 구현 (EXAONE 3.5 + 의안 Q&A) | 해야 할 일 |

> 이 표는 작업 완수 시마다 직접 갱신한다. Jira가 항상 최신 상태의 기준이다.

## GitHub 워크플로우

**원격 저장소**: https://github.com/DKU-OpenSource/BE
**기본 브랜치**: `main`

### 브랜치 전략

작업할 Jira 이슈가 결정되면 아래 흐름을 반드시 따른다.

```
1. main에서 최신 상태로 동기화
   git checkout main && git pull origin main

2. 이슈 번호 기반 브랜치 생성 (소문자, 하이픈 구분)
   git checkout -b feature/KAN-{번호}-{짧은-설명}
   예) feature/KAN-6-initial-setup
       feature/KAN-7-domain-entity
       feature/KAN-8-batch-pipeline

3. 작업 진행 및 커밋
   - 커밋 메시지 형식: [KAN-{번호}] 변경 내용 한 줄 요약

4. 작업 완수 후 브랜치 Push
   git push origin feature/KAN-{번호}-{짧은-설명}

5. GitHub PR 생성 (main 타겟)
   - PR 제목: [KAN-{번호}] 이슈 제목
   - PR 본문: .github/PULL_REQUEST_TEMPLATE.md 템플릿 사용
   - Assignee: parksehyn

6. PR 생성 후 Jira 이슈를 완료로 전환
```

### 주의사항

- `main` 브랜치에 직접 커밋하지 않는다
- PR 없이 merge하지 않는다
- 브랜치는 이슈 1개당 1개를 원칙으로 한다
