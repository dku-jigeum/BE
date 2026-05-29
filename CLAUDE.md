---
level: 1
mode: committed
status: active
last_verified: 2026-05-29
confidence: 0.9
owners: [박세현]
description: Claude Code가 매 세션 읽는 정체성·원칙·규칙. 작업 시작 전 자동 로드.
---

# CLAUDE.md — jigeumchamyeo-backend

> Spring Boot 기반 국회 의안·청원·입법예고 수집·추천·에이전트 백엔드.
> 팀: 1인 + AI (parksehyn)

---

## 0. 저장소 구성

| 디렉터리 | 역할 |
|---|---|
| `src/` | Spring Boot 애플리케이션 소스 |
| `docs/` | 시스템 설계·도메인·워크플로우 문서 |
| `journal/` | 하루 작업 로그 · 세션 인수인계 |
| `.claude/` | AI 하네스 — 서브에이전트·훅·스크립트·권한 |

빌드/테스트는 `README.md` 기준. 이 파일에 반복하지 않는다.

**운영 매뉴얼**: [`docs/WORKFLOW.md`](docs/WORKFLOW.md)

---

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

- 가정을 명시적으로 말한다. 불확실하면 묻는다.
- 해석이 여러 가지면 모두 제시. 조용히 하나를 고르지 않는다.
- 더 단순한 길이 있으면 말한다.
- 모호하면 멈추고 무엇이 모호한지 짚고 묻는다.

## 2. Simplicity First

- 요청되지 않은 기능 금지.
- 단일 호출 코드에 추상화 금지.
- "시니어가 보면 과설계라 할까?" yes이면 단순화.

## 3. Surgical Changes

- 주변 코드를 "개선"하지 않는다.
- 망가지지 않은 것을 리팩터링하지 않는다.
- 네 변경으로 생긴 고아(import/변수/메서드)만 정리.

## 4. Goal-Driven Execution

검증 가능한 목표로 바꾼다:
- "버그 수정" → 재현 테스트 작성 → 통과
- "기능 추가" → 검증 기준 먼저 → 구현 → `./gradlew test`

다단계 작업이면 계획 먼저:
```
1. [단계] → 검증: [확인 방법]
2. [단계] → 검증: [확인 방법]
```

**최종 검증**: `./gradlew test` · `./gradlew build`

---

## 5. 문서 포인터 (docs/)

| 주제 | 문서 | 언제 읽는가 |
|---|---|---|
| **팀 워크플로우 · 정합성 통제 (L1)** | [`docs/WORKFLOW.md`](docs/WORKFLOW.md) | 작업 루프·브랜치·규칙 — 작업 시작 전 |
| 시스템 아키텍처 | [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | 패키지 구조·기술스택·데이터 흐름 변경 시 |
| 도메인 모델 · DB 스키마 | [`docs/DOMAIN_DESIGN.md`](docs/DOMAIN_DESIGN.md) | Entity·마이그레이션·Repository 변경 시 |
| 변경 이력 | [`docs/CHANGELOG.md`](docs/CHANGELOG.md) | 모든 변경의 종착지 |
| 미완료·할 일 | [`docs/TODO.md`](docs/TODO.md) | Jira 연동·외부 설정 체크리스트 |

하루 작업 로그: [`journal/`](journal/) — 어제 흐름 이어받기

---

## 6. 변경 기록 규칙 (필수)

의미 있는 변경마다:

1. **관련 docs 문서 동기화**
   - Entity/스키마 변경 → `docs/DOMAIN_DESIGN.md` 갱신
   - 패키지 구조·API 변경 → `docs/ARCHITECTURE.md` 갱신
   - 외부 설정 추가 → `docs/TODO.md` 체크리스트 추가

2. **`docs/CHANGELOG.md` 맨 위에 한 줄 추가**
   ```
   ## 2026-05-29 — backend
   - FCM 알림 구현 (D-7/D-3/D-1)
   ```

---

## 7. 프로젝트 규칙 (Key Rules)

- Entity는 반드시 `domain/` 패키지 안에 위치
- API 응답은 `ApiResponse<T>` 공통 래퍼 사용
- 공공 API 호출은 `batch/step/reader/` 의 ItemReader에서만
- pgvector 쿼리는 Native Query (`@Query(nativeQuery = true)`)
- 환경변수는 절대 하드코딩 금지 — `application-secret.yml` 사용 (git 제외)
- Batch Job은 멱등성 보장 (중복 실행 시 데이터 중복 없음)
- 임베딩 모델(추천용)과 EXAONE(에이전트용) 역할 분리 — 추천에 EXAONE 사용 금지
- ReAct 루프는 `agent/react/` 에서만 구현
- LLM 모델 호출은 `agent/model/` 에서만
- Agent Tool 정의는 `agent/tool/` 에 위치

## 8. Gotchas

- pgvector: DB 초기화 시 `CREATE EXTENSION IF NOT EXISTS vector;` 필요 (V1 마이그레이션 포함)
- 공공 API 일별 호출 한도 — Batch에서 불필요한 중복 호출 금지
- FCM 토큰은 앱 재설치 시 변경 — `notification/` 갱신 로직 확인 필요
- Spring Batch `JobRepository`는 PostgreSQL 스키마 사용 → 테스트는 Testcontainers
- Batch 수동 실행: `--spring.batch.job.enabled=true` 필수 (없으면 JobLauncherApplicationRunner 미생성)
- Flyway 자동 실행 안 되는 상황 있음 — 프로덕션 배포 전 `flyway_schema_history` 확인
- `PetitionApiItemReader` ERACO 파라미터: 한글 직접 쓰면 이중 인코딩 → `{eraco}` URI 템플릿 변수로 분리
- `BillSummaryItemReader`: 대량 호출 시 `Thread.sleep(500)` 적용
- Batch 동일 파라미터 완료 Job 재실행: `--run.id=숫자` 고유 파라미터 추가 필요

---

## 9. Jira 작업 워크플로우

**보드**: https://dankook-opensource-project.atlassian.net/jira/software/projects/KAN/boards/1
**담당자**: parksehyn (dhaprk0429@dankook.ac.kr)
**Cloud ID**: `2a985da6-fb1b-48d0-8fee-8909b62d0ebb`

1. 세션 시작 — `project = KAN AND assignee = parksehyn AND status = "해야 할 일"` JQL로 미완료 조회, 이슈 번호 오름차순으로 순서대로 완수
2. 작업 완수 후 — Jira 이슈를 `완료` 상태로 전환
3. 모든 작업 완수 시 — 사용자에게 알리고, 신규 이슈 등록 상의 후 진행

---

## 10. GitHub 워크플로우

**원격 저장소**: https://github.com/DKU-OpenSource/BE
**기본 브랜치**: `main`

```
1. git checkout main && git pull origin main
2. git checkout -b feature/KAN-{번호}-{짧은-설명}
3. 커밋 형식: [KAN-{번호}] 변경 내용 한 줄 요약
4. git push origin feature/KAN-{번호}-{짧은-설명}
5. PR 생성 (main 타겟, PULL_REQUEST_TEMPLATE.md 사용)
6. PR 생성 후 Jira 이슈 완료 전환
```

- `main` 직접 커밋 금지 / PR 없이 merge 금지 / 브랜치 1이슈 1개 원칙

---

## 11. 하네스 — 서브에이전트 · 훅 · 스크립트

**서브에이전트 (코드 수정은 부모 Claude만):**
- `architect` — 설계·플랜 (read-only)
- `code-reviewer` — diff 리뷰, Claude 자체 분석 (수정 없음)
- `qa-validator` — 테스트·동작검증 (수정 없음)
- `doc-guardian` — 작업 전 문서 정합성 스캔 (read-only)

**Hooks (`.claude/hooks/`):**
- `session-start.sh` — D-day + 직전 journal "다음 할 일" 출력
- `pre-bash.sh` — 위험 Bash 패턴 차단
- `post-edit-md.sh` — `.md` 수정 후 front-matter 누락 알림
- `stop-summary.sh` — 세션 종료 전 CHANGELOG 누락 알림
- `pre-commit-reminder.sh` — 커밋 전 review/qa 환기 넛지

**Scripts (`.claude/scripts/`):**
- `ship-precheck.sh` — `/ship` 사전 체크 (테스트·린트·타입·CHANGELOG·git status)
- `audit.sh` — 문서 신선도·정합성 감사

**슬래시커맨드:** `/daily` `/handoff` `/ship` `/audit` `/codify`

**표준 작업 루프:**
```
업무지시 → doc-guardian(모순 체크) → architect(플랜)
        → 부모(코드 수정) → code-reviewer → qa-validator → 반복
완료 → 문서 동기화 → /ship → 커밋 → PR
```
