#!/usr/bin/env bash
# session-start.sh — 세션 시작 시 컨텍스트 출력

set -euo pipefail

ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
JOURNAL_DIR="$ROOT/journal"

echo "=== jigeumchamyeo-backend 세션 시작 ==="

# 최신 journal 파일의 "다음 할 일" 섹션 출력
if [ -d "$JOURNAL_DIR" ]; then
  LATEST=$(ls -t "$JOURNAL_DIR"/*.md 2>/dev/null | grep -v README | head -1 || true)
  if [ -n "$LATEST" ]; then
    echo ""
    echo "📋 직전 journal: $(basename "$LATEST")"
    # "다음 할 일" 섹션 추출
    awk '/^## 다음 할 일/{found=1; next} found && /^##/{exit} found{print}' "$LATEST" | head -10
  fi
fi

# git status 간단히
echo ""
echo "🌿 브랜치: $(git branch --show-current 2>/dev/null || echo '알 수 없음')"
UNCOMMITTED=$(git status --porcelain 2>/dev/null | wc -l | tr -d ' ')
if [ "$UNCOMMITTED" -gt 0 ]; then
  echo "⚠️  미커밋 변경: ${UNCOMMITTED}개"
fi

echo "========================================="
