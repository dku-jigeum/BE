---
level: 1
mode: committed
status: active
last_verified: 2026-05-29
confidence: 0.9
owners: [박세현]
description: 1인 + AI 협업 운영 매뉴얼. 작업 루프·브랜치·정합성 통제·문서 레벨의 단일 원천. 작업 시작 전 필독.
---

# WORKFLOW — 운영 매뉴얼 (L1)

> jigeumchamyeo-backend — 1인(박세현) + AI 협업. 갈라질 수 있는 결정을 통제해 충돌·모호함을 없앤다.
> L1 문서: 사람 명시 승인 없이 AI가 수정하지 않는다.

---

## 1. 정합성 통제 티어 (⓪ > ① > ② > ③)

기계로 막을 수 있는 것을 ③(문서)에 두지 않는다.

| 티어 | 원리 | 구현 |
|---|---|---|
| **⓪ 불가능** | 단일 원천에서 생성 | — (현재 없음) |
| **① 자동 결정** | 포매터 | Google Java Format / IDE formatter |
| **② 자동 검출** | 린터·타입·훅 | `./gradlew build` (컴파일 오류), `.claude/hooks/pre-bash.sh` |
| **③ AI 판단** | 문서 + 서브에이전트 | 이 문서 규칙 + `code-reviewer`/`doc-guardian` |

---

## 2. 문서 레벨 + 정합성 폭포

| 레벨 | 성격 | 문서 | 수정 규칙 |
|---|---|---|---|
| **L1** 불변 기반 | 워크플로우 | `WORKFLOW.md`(이 문서) | 사람 명시 승인 필요 |
| **L2** 바뀌면 골치 | 아키텍처·도메인 | `ARCHITECTURE.md`, `DOMAIN_DESIGN.md` | 코드 변경과 짝지어 동기화 |
| **L3** 자주 바뀜 | 로그·할 일 | `TODO.md`, `CHANGELOG.md`, `journal/*` | 자유 |

**폭포 규칙:**
- DB 스키마 변경(L2) → `DOMAIN_DESIGN.md` + `ARCHITECTURE.md` + `README.md` 동기화
- 패키지 구조 변경(L2) → `ARCHITECTURE.md` 갱신

---

## 3. 작업 루프

```
Flow A — 구현
업무 지시
  → [doc-guardian]  문서 정합성 체크 (모호하면 STOP)
  → [architect]     검증 가능한 플랜 작성
  → [부모 Claude]   코드 수정
  → [code-reviewer] Claude 직접 리뷰
  → [qa-validator]  ./gradlew test 확인
  → 통과까지 반복

Flow B — 완료
  → 문서 동기화 (DOMAIN_DESIGN / ARCHITECTURE / CHANGELOG)
  → /ship           최종 게이트
  → 커밋 ([KAN-{번호}] 형식)
  → PR 생성 + Jira 완료 전환
```

---

## 4. 서브에이전트

| 에이전트 | 권한 | 역할 |
|---|---|---|
| `architect` | read-only | 설계·플랜 |
| `code-reviewer` | read+Bash | diff 리뷰 (Claude 직접 분석 — codex 없음) |
| `qa-validator` | read+Bash | 테스트·동작검증 |
| `doc-guardian` | read-only | 작업 전 문서 정합성 스캔 |

**코드 수정은 부모 Claude만.** 서브에이전트 출력 첫 줄: `## VERDICT: <PASS|FAIL|STOP|OK> — <핵심 한 줄>`

---

## 5. 브랜치 · 커밋

- **트렁크 기반**: `main` + 짧은 feature 브랜치
- **브랜치명**: `feature/KAN-{번호}-{짧은-설명}`
- **커밋**: `[KAN-{번호}] 변경 내용 한 줄 요약`
- `main` 직접 커밋 금지 / force-push 금지 (`.claude/hooks/pre-bash.sh` 차단)
- 1 PR = 1 Jira 이슈

---

## 6. 슬래시커맨드

| 커맨드 | 언제 |
|---|---|
| `/daily` | 오늘 작업 journal 기록 |
| `/handoff` | 미완 작업 인수인계 문서 생성 |
| `/ship` | 커밋 전 최종 게이트 |
| `/audit` | 문서 신선도 감사 |
| `/codify` | 발견한 갈라짐을 규칙으로 굳히기 |

---

## 7. 협업 리듬

**하루:**
- 시작: `git pull` → 최신 `journal/` + Jira 미완료 조회
- 작업: feature 브랜치, Flow A 루프
- 종료: `/ship` → 커밋·PR → Jira 전환 → 미완이면 `/handoff` + `/daily`
