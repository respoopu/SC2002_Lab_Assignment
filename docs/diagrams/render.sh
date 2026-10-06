#!/usr/bin/env bash
# Renders every diagram in this folder to PNG and SVG.
# Needs Java and plantuml.jar (https://plantuml.com/download); no Graphviz needed.
#   PLANTUML_JAR=/path/to/plantuml.jar docs/diagrams/render.sh
set -euo pipefail
cd "$(dirname "$0")"
JAR="${PLANTUML_JAR:?set PLANTUML_JAR to the path of plantuml.jar}"
rm -f ./*.png ./*.svg ./*.cmapx
for fmt in png svg; do
  java -DPLANTUML_LIMIT_SIZE=30000 -jar "$JAR" -t"$fmt" \
    class-diagram.puml class-diagram-overview.puml sequence-diagram.puml
  # PlantUML names the pages of a multi-page diagram name, name_001, name_002...; number them 1-4 instead.
  mv "sequence-diagram.$fmt" "sequence-diagram-1.$fmt"
  for i in 1 2 3; do
    mv "sequence-diagram_00$i.$fmt" "sequence-diagram-$((i + 1)).$fmt"
  done
done
