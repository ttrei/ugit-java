#!/usr/bin/env sh

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")

root_dir=$(pwd)

cd "$SCRIPT_DIR" || exit 1

if [ -n "$*" ]; then
    "$SCRIPT_DIR/gradlew" run -q --args="--root $root_dir $*"
else
    "$SCRIPT_DIR/gradlew" run -q --args="--root $root_dir"
fi
