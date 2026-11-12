#!/bin/bash
cd "$(dirname "$0")/.."
name=$(basename "$1"); mkdir -p run/logs-test
rm -rf run/server/world run/server/addons/lucky_block run/server/addons/lucky_legacy; mkdir -p run/server/addons/lucky_block
for a in "$@"; do cp -r "$a" run/server/addons/lucky_block/; done
log=run/logs-test/$name.log
JAVA_TOOL_OPTIONS="-Dfml.queryResult=confirm" ./gradlew runServer --no-daemon > "$log" 2>&1 &
for i in $(seq 1 120); do sleep 3; grep -q -E 'Done \(|BUILD FAILED|Crash report' "$log" && break; done
python -I tools/rcon.py stop >/dev/null 2>&1
for i in $(seq 1 20); do sleep 2; grep -q -E 'BUILD SUCCESSFUL|BUILD FAILED|Stopping server' "$log" && break; done
sleep 3; true
