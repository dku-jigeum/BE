---
description: 발견된 갈라짐(네이밍·패턴·결정)을 가장 싼 통제 티어의 산출물로 굳혀 다음 세션이 상속하게 한다
---

# /codify

사용법: `/codify "<갈라진 결정>"`

## 절차

1. 갈라짐의 종류 판단
   - 기계화 가능 (항상 같은 답) → `CLAUDE.md` Key Rules에 한 줄 추가
   - 판단 영역 (맥락에 따라 다름) → `docs/WORKFLOW.md` 에 한 줄

2. `docs/CHANGELOG.md` 에 `codify: <결정 내용>` 한 줄 추가

## 예시

- `/codify "get_ vs fetch_ 메서드 접두사"` → Key Rules에 통일 규칙 추가
- `/codify "Batch Job 파라미터 재실행 방법"` → Gotchas에 추가
