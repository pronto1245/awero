#!/usr/bin/env bash
set -euo pipefail
shots=(Create HomeLargeText Progress Profile Math)
for language in en ru pt-BR fr de es; do
  for screen in Home Progress Profile Create; do shots+=("$screen-$language"); done
done
capture_file=$(mktemp)
trap 'rm -f "$capture_file"' EXIT
for shot in "${shots[@]}"; do
  adb pull "/data/local/tmp/awero-visual-$shot.png" "$capture_file" >/dev/null
  test "$(od -An -tx1 -N8 "$capture_file" | tr -d '[:space:]')" = "89504e470d0a1a0a"
  base64 -w0 "$capture_file" | fold -w3000 |
    awk -v name="$shot" '{print "AWERO_VISUAL|android|" name "|" NR-1 "|" $0}'
done
