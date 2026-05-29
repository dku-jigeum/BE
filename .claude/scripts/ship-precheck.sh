#!/usr/bin/env bash
# ship-precheck.sh — /ship 사전 체크
# 사용법: bash .claude/scripts/ship-precheck.sh [backend|docs|all]

set -euo pipefail

SCOPE="${1:-all}"
ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
PASS=0
FAIL=0
TODAY=$(date +%Y-%m-%d)

check_pass() { echo "  ✅ $1"; ((PASS++)) || true; }
check_fail() { echo "  ❌ $1"; ((FAIL++)) || true; }
check_warn() { echo "  ⚠️  $1"; }

echo "=== ship-precheck: $SCOPE ==="

# §1 CHANGELOG 오늘 항목
echo ""
echo "[1] CHANGELOG 오늘 항목"
CHANGELOG="$ROOT/docs/CHANGELOG.md"
if [ -f "$CHANGELOG" ] && grep -q "^## ${TODAY}" "$CHANGELOG"; then
  check_pass "CHANGELOG 오늘($TODAY) 항목 있음"
else
  check_fail "CHANGELOG 오늘($TODAY) 항목 없음 — docs/CHANGELOG.md 에 추가하세요"
fi

# §2 git 상태
echo ""
echo "[2] git 상태"
UNCOMMITTED=$(git status --porcelain 2>/dev/null | wc -l | tr -d ' ')
if [ "$UNCOMMITTED" -eq 0 ]; then
  check_pass "미커밋 변경 없음"
else
  check_warn "미커밋 변경 ${UNCOMMITTED}개 있음 (의도적이면 OK)"
fi

if [ "$SCOPE" = "backend" ] || [ "$SCOPE" = "all" ]; then
  # §3 빌드 확인
  echo ""
  echo "[3] ./gradlew build (컴파일)"
  if (cd "$ROOT" && ./gradlew build -x test --quiet 2>&1 | tail -5); then
    check_pass "빌드 성공"
  else
    check_fail "빌드 실패"
  fi

  # §4 테스트
  echo ""
  echo "[4] ./gradlew test"
  if (cd "$ROOT" && ./gradlew test --quiet 2>&1 | tail -10); then
    check_pass "테스트 통과"
  else
    check_fail "테스트 실패"
  fi
fi

if [ "$SCOPE" = "docs" ] || [ "$SCOPE" = "all" ]; then
  # §5 docs front-matter 필수 필드 확인
  echo ""
  echo "[5] docs front-matter 필수 필드"
  REQUIRED_FIELDS="level mode status last_verified confidence owners description"
  DOCS_FAIL=0
  for doc in "$ROOT/docs"/*.md; do
    [ -f "$doc" ] || continue
    for field in $REQUIRED_FIELDS; do
      if ! head -20 "$doc" | grep -q "^${field}:"; then
        echo "     누락: $field in $(basename "$doc")"
        DOCS_FAIL=1
      fi
    done
  done
  if [ "$DOCS_FAIL" -eq 0 ]; then
    check_pass "docs front-matter 필드 정상"
  else
    check_fail "docs front-matter 누락 필드 있음"
  fi
fi

# 결과
echo ""
echo "==================================="
echo "결과: ✅ $PASS PASS / ❌ $FAIL FAIL"

if [ "$FAIL" -gt 0 ]; then
  echo "❌ 게이트 실패 — 위 항목 수정 후 재실행"
  exit 1
else
  echo "✅ 게이트 통과 — 커밋 진행 가능"
  exit 0
fi
