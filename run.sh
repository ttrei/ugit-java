#!/usr/bin/env sh

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")

if [ -n "$*" ]; then
    "$SCRIPT_DIR/gradlew" run -q --args="$*"
else
    "$SCRIPT_DIR/gradlew" run -q
fi
