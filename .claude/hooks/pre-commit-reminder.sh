#!/usr/bin/env bash
# pre-commit-reminder.sh — 커밋 전 review/qa 환기 넛지 (차단 아님)

set -euo pipefail

COMMAND="${CLAUDE_TOOL_INPUT:-}"

if echo "$COMMAND" | grep -qE 'git commit'; then
  echo "💡 REMINDER: code-reviewer / qa-validator 를 거쳤나요?"
  echo "   사소한 변경(오타·문구·설정)이면 스킵 OK."
  echo "   로직·DB·API 변경이면 review/qa 권장."
fi

exit 0
