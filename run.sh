#!/usr/bin/env bash
# Builds and starts the game. For a scripted run, add --echo so each answer
# is printed after its prompt:   ./run.sh --echo < runs/victory.txt
set -euo pipefail
cd "$(dirname "$0")"
./build.sh
exec java -cp out/main restaurantrush.boundary.Main "$@"
