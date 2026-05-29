#!/usr/bin/env bash
# pre-bash.sh — 위험 Bash 패턴 차단

set -euo pipefail

COMMAND="${CLAUDE_TOOL_INPUT:-}"

# force-push 차단
if echo "$COMMAND" | grep -qE 'git push.*(--force|-f)'; then
  echo "❌ BLOCKED: force-push 금지. 필요하면 사용자에게 직접 확인하세요." >&2
  exit 1
fi

# reset --hard 차단
if echo "$COMMAND" | grep -qE 'git reset --hard'; then
  echo "❌ BLOCKED: git reset --hard 금지. 사용자 확인 필요." >&2
  exit 1
fi

# rm -rf / 차단
if echo "$COMMAND" | grep -qE 'rm\s+-rf\s+/'; then
  echo "❌ BLOCKED: rm -rf / 금지." >&2
  exit 1
fi

# .env / application-secret.yml 직접 출력 차단
if echo "$COMMAND" | grep -qE '(cat|echo|print).*(\.env|application-secret\.yml)'; then
  echo "❌ BLOCKED: 비밀 파일 직접 출력 금지." >&2
  exit 1
fi

exit 0
