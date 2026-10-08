#!/usr/bin/env bash
set -euo pipefail

RUNNER=app.awero.test/androidx.test.runner.AndroidJUnitRunner
TEST_CLASS=app.awero.core.storage.ProcessCrashRecoveryTest
REPORT_DIR=apps/android/app/build/outputs/process-crash
mkdir -p "$REPORT_DIR"
timeout 60s adb install -r apps/android/app/build/outputs/apk/debug/app-debug.apk
timeout 60s adb install -r apps/android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell run-as app.awero rm -f files/awero-crash-ready

timeout 180s adb shell am instrument -w -e class "$TEST_CLASS" -e aweroCrashPhase write "$RUNNER" > "$REPORT_DIR/write.txt" 2>&1 &
WRITER_PID=$!
trap 'kill "$WRITER_PID" 2>/dev/null || true' EXIT
READY=false
for ATTEMPT in $(seq 1 120); do
  if adb shell run-as app.awero test -f files/awero-crash-ready; then
    READY=true
    break
  fi
  if ! kill -0 "$WRITER_PID" 2>/dev/null; then
    cat "$REPORT_DIR/write.txt"
    echo "Android fixture exited before committing data"
    exit 1
  fi
  sleep 0.5
done
if [ "$READY" != true ]; then
  cat "$REPORT_DIR/write.txt"
  exit 1
fi
APP_PID=$(adb shell pidof app.awero | tr -d '\r')
[[ "$APP_PID" =~ ^[0-9]+$ ]]
adb shell run-as app.awero kill -9 "$APP_PID"
wait "$WRITER_PID" || true
trap - EXIT
adb shell dumpsys alarm > "$REPORT_DIR/alarms-after-sigkill.txt"
rg -F 'app.awero.ALARM' "$REPORT_DIR/alarms-after-sigkill.txt"
timeout 120s adb shell am instrument -w -e class "$TEST_CLASS" -e aweroCrashPhase read "$RUNNER" | tee "$REPORT_DIR/read.txt"
rg 'OK \(1 test\)' "$REPORT_DIR/read.txt"
if rg 'FAILURES|INSTRUMENTATION_FAILED|shortMsg=' "$REPORT_DIR/read.txt"; then exit 1; fi
adb logcat -d > "$REPORT_DIR/logcat.txt"
echo "AWERO Android SIGKILL persistence + alarm recovery + E2E: PASS"
