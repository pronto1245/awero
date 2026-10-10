#!/usr/bin/env bash
set -euo pipefail
shots=(Create HomeLargeText Progress Profile Math)
for language in en ru pt-BR fr de es; do
  for screen in Home Progress Profile Create; do shots+=("$screen-$language"); done
done
visual_output=apps/android/app/build/outputs/visual
mkdir -p "$visual_output"
capture_file=$(mktemp)
trap 'rm -f "$capture_file"' EXIT
for shot in "${shots[@]}"; do
  adb pull "/data/local/tmp/awero-visual-$shot.png" "$capture_file" >/dev/null
  test "$(od -An -tx1 -N8 "$capture_file" | tr -d '[:space:]')" = "89504e470d0a1a0a"
  cp "$capture_file" "$visual_output/$shot.png"
  printf 'Verified PNG: %s\n' "$shot"
done
