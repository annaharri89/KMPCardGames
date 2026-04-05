#!/usr/bin/env bash
set -euo pipefail

SCRIPT_LOG_TAG="BinarySizeBudgetCheck"
BUILD_DIR="${1:-build}"
MAX_ALLOWED_BYTES="${2:-26214400}"

if [[ ! -d "$BUILD_DIR" ]]; then
  echo "[$SCRIPT_LOG_TAG] Build directory missing ($BUILD_DIR). Skipping size check before first build."
  exit 0
fi

largest_artifact_size=$(python3 - "$BUILD_DIR" <<'PY'
import os
import sys

build_dir = sys.argv[1]
max_size = 0
for root, _, files in os.walk(build_dir):
    for file_name in files:
        file_path = os.path.join(root, file_name)
        try:
            file_size = os.path.getsize(file_path)
        except OSError:
            continue
        if file_size > max_size:
            max_size = file_size
print(max_size)
PY
)

if [[ "$largest_artifact_size" == "0" ]]; then
  echo "[$SCRIPT_LOG_TAG] No artifacts found in $BUILD_DIR."
  exit 0
fi

if (( largest_artifact_size > MAX_ALLOWED_BYTES )); then
  echo "[$SCRIPT_LOG_TAG] Largest artifact ($largest_artifact_size bytes) exceeded budget ($MAX_ALLOWED_BYTES bytes)."
  exit 1
fi

echo "[$SCRIPT_LOG_TAG] Largest artifact size within budget: $largest_artifact_size bytes."
