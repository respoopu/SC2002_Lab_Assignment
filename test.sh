#!/usr/bin/env bash
# Builds the game, compiles the JUnit tests and runs all of them.
set -euo pipefail
cd "$(dirname "$0")"
JUNIT=lib/junit-platform-console-standalone-1.13.4.jar
./build.sh
rm -rf out/test
mkdir -p out/test
find test -name '*.java' > out/test-sources.txt
javac --release 17 -d out/test -cp "out/main:$JUNIT" @out/test-sources.txt
java -jar "$JUNIT" execute --class-path "out/test:out/main" --scan-class-path --disable-banner
