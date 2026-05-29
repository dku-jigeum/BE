---
description: 커밋/PR 전 최종 게이트 — 빌드·테스트·CHANGELOG·문서 정합성을 한 번에 검증
---

# /ship

사용법: `/ship [backend|docs|all]` (기본값: all)

## 실행 절차

1. `bash .claude/scripts/ship-precheck.sh $ARGUMENTS` 실행
2. FAIL 항목이 있으면 수정 후 재실행
3. 전체 PASS 시 커밋 진행

## 커밋 후

- `git push origin feature/KAN-{번호}-{설명}`
- GitHub PR 생성 (PULL_REQUEST_TEMPLATE.md 사용)
- Jira 이슈 완료 전환
