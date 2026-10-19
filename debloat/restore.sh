#!/bin/bash
# Brings back apps hidden by debloat.sh.
#
# Usage: ./restore.sh <tv-ip> <package>   restore one app
#        ./restore.sh <tv-ip>             restore every app in the list and re-enable the stock launcher
#        LIST=examples/xiaomi-tv.txt ./restore.sh <tv-ip>   use a different list (default: packages.txt)
set -u
if [ $# -lt 1 ]; then
  echo "Usage: $0 <tv-ip> [package]"
  exit 1
fi
S="$1:5555"
LIST="${LIST:-$(dirname "$0")/packages.txt}"

adb connect "$S"
if [ $# -ge 2 ]; then
  adb -s "$S" shell cmd package install-existing "$2"
  exit
fi
adb -s "$S" shell pm enable com.google.android.tvlauncher
sed 's/#.*//; s/[[:space:]]*$//' "$LIST" | grep -v '^$' | while read -r p; do
  echo "$p: $(adb -s "$S" shell -n cmd package install-existing "$p" 2>&1)"
done
