#!/usr/bin/env bash
# post-edit-md.sh — .md 파일 수정 후 front-matter 7필드 누락 알림

set -euo pipefail

FILE_PATH="${CLAUDE_TOOL_INPUT_FILE_PATH:-}"

# .md 파일만 체크
if [[ "$FILE_PATH" != *.md ]]; then
  exit 0
fi

# docs/ 또는 journal/ 안의 파일만 체크
if ! echo "$FILE_PATH" | grep -qE '(docs/|journal/)'; then
  exit 0
fi

REQUIRED_FIELDS=(level mode status last_verified confidence owners description)
MISSING=()

for field in "${REQUIRED_FIELDS[@]}"; do
  if ! head -20 "$FILE_PATH" 2>/dev/null | grep -q "^${field}:"; then
    MISSING+=("$field")
  fi
done

if [ ${#MISSING[@]} -gt 0 ]; then
  echo "⚠️  front-matter 누락 필드: ${MISSING[*]} (파일: $FILE_PATH)"
fi

exit 0
