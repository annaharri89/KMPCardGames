#!/usr/bin/env bash
set -euo pipefail

SCRIPT_LOG_TAG="SharedNoKorlibsCheck"
SHARED_KT_ROOT="shared/src"

if [[ ! -d "$SHARED_KT_ROOT" ]]; then
  echo "[$SCRIPT_LOG_TAG] Directory not found: $SHARED_KT_ROOT"
  exit 1
fi

if grep -R -n --include='*.kt' 'korlibs' "$SHARED_KT_ROOT" 2>/dev/null; then
  echo "[$SCRIPT_LOG_TAG] :shared must not reference korlibs.* (keep KorGE/Korlibs types in :clients:korge only)."
  exit 1
fi

echo "[$SCRIPT_LOG_TAG] No korlibs references under $SHARED_KT_ROOT."
