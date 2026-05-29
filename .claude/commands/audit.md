---
description: 지정 영역의 문서 정합성·신선도 감사
---

# /audit

사용법: `/audit [l1|l2|l3|all]` (기본값: all)

`bash .claude/scripts/audit.sh $ARGUMENTS` 실행.

결과는 알림만 — 차단하지 않는다 (cascade paralysis 방지).
stale 항목은 `last_verified` 갱신 후 `confidence` 재평가.
