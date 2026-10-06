# Diagrams

PlantUML sources and their rendered images for the final design (tag `stage-3`).

| Diagram | Source | Images |
|---|---|---|
| Class diagram: every class, attribute and operation with visibility, relationships with multiplicities, and notes on encapsulation, polymorphism, composition and dependency inversion | `class-diagram.puml` | `class-diagram.svg` / `.png` |
| Class overview: the same diagram with members hidden, sized for one page | `class-diagram-overview.puml` (includes `class-diagram.puml`) | `class-diagram-overview.svg` / `.png` |
| Sequence diagram: the Critic's visit in the Stage 3 victory run (turns 7-11) and the result check, in four parts | `sequence-diagram.puml` | `sequence-diagram-1` to `-4` `.svg` / `.png` |

The SVGs stay sharp at any zoom, so use them in the report where the editor accepts SVG. The PNGs are rendered at 150 dpi.

## Re-rendering

Edit the `.puml` source, then render with Java and [plantuml.jar](https://plantuml.com/download). Graphviz is not needed.

```bash
PLANTUML_JAR=/path/to/plantuml.jar docs/diagrams/render.sh
```

The class diagram was checked member by member against `javap -p` output for the compiled `stage-3` classes. If a class changes, update the diagram in the same commit.
