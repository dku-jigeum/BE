---
level: 2
mode: committed
status: active
last_verified: 2026-06-02
confidence: 0.9
owners: [박세현]
description: BE 고유 규칙 — 공통 원칙·docs는 루트 CLAUDE.md 참조.
---

# CLAUDE.md — jigeumchamyeo-backend (BE)

> **공통 원칙·워크플로우·설계 문서는 루트를 읽어라:**
> - 원칙: `../CLAUDE.md`
> - 문서: `../docs/` (WORKFLOW, ARCHITECTURE, DOMAIN_DESIGN, CHANGELOG, TODO)
> - 에이전트·훅: `../.claude/`

---

## 1. Key Rules (BE 고유)

- Entity는 반드시 `domain/` 패키지
- API 응답은 `ApiResponse<T>` 공통 래퍼
- 공공 API 호출은 `batch/step/reader/` ItemReader에서만
- pgvector 쿼리는 Native Query (`@Query(nativeQuery = true)`)
- 환경변수는 `application-secret.yml` (git 제외) — 절대 하드코딩 금지
- Batch Job은 멱등성 보장
- 임베딩 모델(추천) vs EXAONE(에이전트) 역할 분리 — 혼용 금지
- ReAct 루프는 `agent/react/`, LLM 호출은 `agent/model/`, Tool은 `agent/tool/`

## 2. Gotchas

- pgvector: `CREATE EXTENSION IF NOT EXISTS vector;` (V1 마이그레이션)
- 공공 API 일별 호출 한도 — 중복 호출 금지
- Batch 수동 실행: `--spring.batch.job.enabled=true` 필수
- Batch 동일 파라미터 완료 Job 재실행: `--run.id=숫자` 추가 필요
- PetitionApiItemReader ERACO 파라미터: `{eraco}` URI 템플릿 변수로 분리 (이중 인코딩 방지)
- BillSummaryItemReader 대량 호출: `Thread.sleep(500)` 적용

## 3. 빌드 · 검증

```bash
./gradlew test      # 단위·통합 테스트
./gradlew build     # 컴파일 + 테스트
```

## 4. Jira 워크플로우

**보드**: https://dankook-opensource-project.atlassian.net/jira/software/projects/KAN/boards/1
**Cloud ID**: `2a985da6-fb1b-48d0-8fee-8909b62d0ebb`

1. 세션 시작 — `status = "해야 할 일"` JQL 조회, 번호 오름차순 완수
2. 완수 후 — Jira `완료` 전환

## 5. GitHub 워크플로우

**저장소**: https://github.com/DKU-OpenSource/BE  
**브랜치명**: `feature/KAN-{번호}-{설명}`  
**커밋**: `[KAN-{번호}] 변경 내용 한 줄 요약`
