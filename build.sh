#!/usr/bin/env sh

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
rm -f "$SCRIPT_DIR"/build/libs/*.jar
"$SCRIPT_DIR"/gradlew shadowJar
