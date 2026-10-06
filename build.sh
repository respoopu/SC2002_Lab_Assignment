#!/usr/bin/env bash
# Compiles all game sources into out/main (Java 17 language level).
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out/main
mkdir -p out/main
find src -name '*.java' > out/main-sources.txt
javac --release 17 -d out/main @out/main-sources.txt
