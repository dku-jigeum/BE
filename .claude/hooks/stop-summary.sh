#!/usr/bin/env bash
# stop-summary.sh — 세션 종료 전 CHANGELOG 누락 알림

set -euo pipefail

ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
CHANGELOG="$ROOT/docs/CHANGELOG.md"

TODAY=$(date +%Y-%m-%d)

# CHANGELOG에 오늘 날짜 항목이 있는지 확인
if [ -f "$CHANGELOG" ]; then
  if ! grep -q "^## ${TODAY}" "$CHANGELOG"; then
    echo "⚠️  CHANGELOG 누락: 오늘($TODAY) 항목이 없습니다. docs/CHANGELOG.md 에 한 줄 추가하세요."
  fi
fi

exit 0
