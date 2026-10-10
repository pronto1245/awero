#!/usr/bin/env bash
set -euo pipefail
for shot in Create HomeLargeText Progress Profile Math; do
  adb exec-out run-as app.awero cat "cache/awero-visual/$shot.png" |
    base64 -w0 | fold -w3000 |
    awk -v name="$shot" '{print "AWERO_VISUAL|android|" name "|" NR-1 "|" $0}'
done
