#!/usr/bin/env bash
set -euo pipefail

SCRIPT_LOG_TAG="ClientBoundaryCheck"
CLIENT_CODE_DIR="clients/korge/src/commonMain/kotlin"

if [[ ! -d "$CLIENT_CODE_DIR" ]]; then
  echo "[$SCRIPT_LOG_TAG] Client directory not found: $CLIENT_CODE_DIR"
  exit 1
fi

if grep -R -n -E "domain\\.rules|RuleSet|CardStackPolicies|DeterministicStateReducer" "$CLIENT_CODE_DIR"; then
  echo "[$SCRIPT_LOG_TAG] Client boundary violation detected. Move rule logic to :shared."
  exit 1
fi

echo "[$SCRIPT_LOG_TAG] Client boundary check passed."
