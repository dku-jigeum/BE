#!/usr/bin/env bash
# audit.sh — 문서 신선도·정합성 감사
# 사용법: bash .claude/scripts/audit.sh [l1|l2|l3|all]

set -euo pipefail

SCOPE="${1:-all}"
ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
TODAY=$(date +%Y-%m-%d)

echo "=== audit: $SCOPE ==="

audit_file() {
  local file="$1"
  local basename
  basename=$(basename "$file")

  # last_verified 추출
  local last_verified
  last_verified=$(grep "^last_verified:" "$file" 2>/dev/null | awk '{print $2}' | tr -d '"' || echo "")

  # confidence 추출
  local confidence
  confidence=$(grep "^confidence:" "$file" 2>/dev/null | awk '{print $2}' || echo "")

  # 30일 이상 오래됐으면 stale 경고
  if [ -n "$last_verified" ]; then
    # 날짜 비교 (macOS/Linux 호환)
    local days_old=0
    if command -v python3 &>/dev/null; then
      days_old=$(python3 -c "
from datetime import date
try:
    d = date.fromisoformat('$last_verified')
    print((date.today() - d).days)
except:
    print(0)
")
    fi
    if [ "$days_old" -gt 30 ]; then
      echo "  ⚠️  STALE ($days_old 일): $basename (last_verified: $last_verified)"
    fi
  fi

  # confidence < 0.7 경고
  if [ -n "$confidence" ]; then
    local low_conf
    low_conf=$(python3 -c "print('yes' if float('$confidence') < 0.7 else 'no')" 2>/dev/null || echo "no")
    if [ "$low_conf" = "yes" ]; then
      echo "  ⚠️  LOW CONFIDENCE ($confidence): $basename"
    fi
  fi
}

# 레벨별 파일 감사
for doc in "$ROOT/docs"/*.md; do
  [ -f "$doc" ] || continue
  level=$(grep "^level:" "$doc" 2>/dev/null | awk '{print $2}' || echo "")
  case "$SCOPE" in
    l1) [ "$level" = "1" ] && audit_file "$doc" ;;
    l2) [ "$level" = "2" ] && audit_file "$doc" ;;
    l3) [ "$level" = "3" ] && audit_file "$doc" ;;
    all) audit_file "$doc" ;;
  esac
done

# CLAUDE.md
audit_file "$ROOT/CLAUDE.md" 2>/dev/null || true

echo ""
echo "감사 완료. ⚠️ 항목은 알림만 — 작업은 계속 진행 가능."
