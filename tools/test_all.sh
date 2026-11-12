#!/bin/bash
cd "$(dirname "$0")/.."
for a in run/addons-src/*; do tools/test_addon.sh "$a"; done
echo done > run/logs-test/_ALL_DONE
