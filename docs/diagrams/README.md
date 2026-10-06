# Diagrams

PlantUML sources and rendered images for the final design (code at tag `stage-3`).

## What to use where

| Diagram | Images | Contents |
|---|---|---|
| Class overview | `class-diagram-overview` | Every class and its structural relationships (generalizations, associations, compositions, aggregations and the key dependencies) on one page, with the annotations on encapsulation, polymorphism, composition and dependency inversion. Members, multiplicities and secondary dependencies are left out so it stays readable. |
| Class detail pages | `class-detail-1-boundary`, `class-detail-2-control`, `class-detail-3-restaurant-staff`, `class-detail-4-customers-orders-menu` | Attributes and operations with visibility, associations with multiplicities, and dependencies, one area of the code per page. Each page draws the links owned by its own classes; classes from other pages appear as collapsed boxes. Simple getters are left out here (stated in each legend). |
| Full class diagram | `class-diagram` | Everything on one canvas, including the simple getters. Too large to print; use the SVG and zoom in, or put it in an appendix. |
| Sequence diagram | `sequence-diagram-1` to `-10` | The Critic's visit in the Stage 3 victory run (turns 7-11) and the result check, in ten parts sized for landscape pages; each part's title names the turn and step it shows. |

Each diagram comes as `.svg` (sharp at any zoom; use it where the editor accepts SVG) and `.png` (150 dpi). The detail and sequence pages are meant for landscape pages. On A4 their text prints at roughly 4-6 pt, so the PDF should embed the SVG or full-resolution PNG (readable when zoomed); for paper, A3 landscape gives about 1.4 times larger text.

## Sources

| File | Role |
|---|---|
| `class-model.iuml` | Every class, member and structural relationship, defined once. |
| `class-dependencies.iuml` | Secondary dependencies (calls and creations that are not structural links). |
| `class-notes-*.iuml`, `class-legend.iuml` | The annotations and the legend. |
| `class-diagram.puml`, `class-diagram-overview.puml`, `class-detail-*.puml` | The class diagrams: each includes the model and chooses what to show. |
| `sequence-diagram.puml` | The sequence diagram (all ten parts). |

To change a class, edit `class-model.iuml`; every class diagram picks it up.

## Re-rendering

Render with Java and [plantuml.jar](https://plantuml.com/download). Graphviz is not needed.

```bash
PLANTUML_JAR=/path/to/plantuml.jar docs/diagrams/render.sh
```

## How the diagrams were checked

- The class members (names, visibility, static/abstract/final, parameter names and types, return types) were compared with the compiled `stage-3` classes using reflection. Members not drawn: the compiler-generated ones listed in the legend, and on the detail pages the simple getters.
- Every class-diagram SVG was checked by a script for labels overlapping other text or line markers, labels crossed by another line or a package border, line ends sitting on another link's diamond or triangle, and multiplicities nearer another line's end than their own. Detail page 4 is clean; detail pages 1-3 have one to four minor cases each (a label touching a package border; on page 2 the composition's "1" at Scoreboard sits beside the arrowhead of GameConfig's dependency).
- Known layout issues still open: on the overview and the full diagram some line ends meet on top of another link's marker (Restaurant's diamonds, GameController's Scoreboard diamond, and ComboMeal's first/second links ending at MenuItem's inheritance triangle). Read those relationships from the detail pages, which draw them separately.
- The sequence diagram pages were checked for overlapping labels (none).
- Every value in the sequence diagram was checked against the transcript of `runs/victory.txt`, and the alternative outcomes against the `runs/what-if-*.txt` runs.
