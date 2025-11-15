#!/usr/bin/env sh

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")

cd "$SCRIPT_DIR" || exit 1

if [ -n "$*" ]; then
    "$SCRIPT_DIR/gradlew" run -q --args="$*"
else
    "$SCRIPT_DIR/gradlew" run -q
fi
