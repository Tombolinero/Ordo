#!/usr/bin/env bash
# Runs inside the Android emulator job (see .github/workflows/emulator.yml).
#   old.apk  – app built with a lower version
#   new.apk  – app built from this commit
#   test.apk – instrumentation tests
set -euo pipefail
APP=io.github.tombolinero.ordo.debug
RUNNER=$APP.test/androidx.test.runner.AndroidJUnitRunner
mkdir -p out

instrument() { # $1 = name, rest = extra args
  local name=$1; shift
  adb shell am instrument -w "$@" "$RUNNER" | tee "out/$name.txt"
  grep -q "^OK (" "out/$name.txt"
}

echo "::group::Update over an older version keeps all data"
adb install -r old.apk
adb install -r test.apk
instrument update-seed -e class io.github.tombolinero.ordo.UpdateTest#seed -e ordoUpdateStep seed
adb install -r new.apk
adb shell dumpsys package $APP | grep -E "versionCode|versionName" | tee out/version.txt
instrument update-verify -e class io.github.tombolinero.ordo.UpdateTest#verify -e ordoUpdateStep verify
echo "::endgroup::"

echo "::group::Shrunk release build starts and survives random input"
CHECK=io.github.tombolinero.ordo.check
adb install -r minified.apk
adb logcat -c
adb shell am start -W -n $CHECK/io.github.tombolinero.ordo.MainActivity | tee out/minified-start.txt
sleep 3
adb shell monkey -p $CHECK --throttle 150 --pct-syskeys 0 --pct-appswitch 0 -s 42 400 | tee out/monkey.txt
adb logcat -d > out/logcat-minified.txt
if grep -E "FATAL EXCEPTION|ANR in $CHECK" out/logcat-minified.txt; then
  echo "::error::The shrunk release build crashed (see out/logcat-minified.txt)"; exit 1
fi
adb uninstall $CHECK || true
echo "::endgroup::"

echo "::group::End-to-end flow (recorded)"
adb shell pm clear $APP
adb shell "screenrecord --bit-rate 8000000 /sdcard/ordo-demo.mp4" &
REC=$!
sleep 2
set +e
instrument flow -e class io.github.tombolinero.ordo.OrdoFlowTest -e ordoDemo true
RESULT=$?
set -e
sleep 2
# Stop the recorder on the device so it finalises the MP4 properly.
adb shell pkill -INT screenrecord || true
wait $REC 2>/dev/null || true
sleep 2
adb pull /sdcard/ordo-demo.mp4 out/ordo-demo.mp4 || true
adb exec-out screencap -p > out/last-screen.png || true
echo "::endgroup::"
exit $RESULT
