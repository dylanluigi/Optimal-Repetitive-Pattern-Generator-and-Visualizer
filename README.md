# Fractals & Tiling Visualizer (MVC + Concurrency)

A compact, teaching-oriented Java app to **generate and visualize self-similar geometry and tessellations**. It includes a Swing GUI, an event-driven **MVC** architecture, and both **animated (sequential)** and **parallel (Fork/Join)** implementations of classic patterns: **Tromino tiling**, **Domino tiling**, **Hilbert curve**, **Koch curve**, and **Sierpiński triangle**.&#x20;

> Built for an Advanced Algorithms course. The accompanying (Catalan) report explains design choices, concurrency model, and asymptotic costs.&#x20;

---

## Features

* **Interactive GUI** (Swing + AWT):

  * Choose **algorithm** (Tromino, Domino, Hilbert, Koch, Sierpiński).
  * Set **board size** / **recursion depth** with guardrails (powers of two for tilings).
  * For Tromino, pick the **missing tile** by clicking or typing coordinates.
  * Toggle **Animated** (step-by-step) vs **Parallel** (compute fast, no animation).&#x20;
* **Clean MVC, event-driven**:

  * Algorithms implement a common `TilingAlgorithm` contract.
  * Model emits progress via a **notification interface**; the controller translates events into GUI updates (decoupled from the View).&#x20;
* **Concurrency done right**:

  * **Animation** on a scheduled executor (keeps UI responsive).
  * **Computation** on **ForkJoinPool** (work stealing; no explicit locks).&#x20;
* **Didactic focus** on **Divide-and-Conquer**, **asymptotics**, and practical parallelism trade-offs.&#x20;

---

## Architecture (MVC + events)

```
View (Swing/AWT)
  └─ GUITiling
        ↑   user actions
        ↓   draw updates
Controller
  └─ TilingController
        ↑   model events (start/step/done/error)
        ↓   run/dispatch tasks
Model (Algorithms)
  ├─ TrominoTiling
  ├─ DominoTiling
  ├─ HilbertCurve
  ├─ KochCurve
  └─ SierpinskiTriangle
        ↳ implements TilingAlgorithm
```

* **Event flow:** model notifies via a small interface (e.g., `onTilingStarted`, `onTilingStep`, `onTilingCompleted`), controller relays updates to the view; computation never blocks the EDT.&#x20;
* **Concurrency:** animation uses a **ScheduledExecutorService**; heavy work uses a **ForkJoinPool** (divide-and-conquer subtasks). No explicit locks/monitors required.&#x20;

---

## Algorithms & complexity (big-O)

* **Tromino tiling (2^k × 2^k board, one missing cell)**:
  Divide into 4 quadrants, place central L-tromino, recurse → **O(n²)** work, stack depth **O(log n)**.&#x20;
* **Domino tiling (board covering variants)**:
  Similar board/recursion scaffolding where applicable; rendered via the common tiling pipeline.&#x20;
* **Hilbert curve (order *d*)**:
  4 subcurves per level → **T(d)=4T(d−1)+O(1) ⇒ O(4^d)** (≈ O(n²) if side n=2^d).&#x20;
* **Koch curve (depth *n*)**:
  Each segment spawns 4 → **O(4^n)** segments, linear work in produced segments.&#x20;
* **Sierpiński triangle (depth *n*)**:
  3 recursive subproblems per level → **O(3^n)** segments/triangles.&#x20;

> Parallelism reduces **wall-clock time** but not total asymptotic work; overheads follow Amdahl’s law—small depths may see little benefit.&#x20;

---


##  Key classes (what they do)

| Class/File           | Role                                                                    |
| -------------------- | ----------------------------------------------------------------------- |
| `GUITiling`          | **View**. Collects user inputs, hosts controls/canvas, triggers runs.   |
| `BoardPanel`         | **Canvas**. Renders grids, curves, and tilings with AWT primitives.     |
| `TilingAlgorithm`    | **Model contract**. Common API implemented by all algorithms.           |
| `TrominoTiling`      | L-tromino Divide-and-Conquer; supports animated or parallel execution.  |
| `DominoTiling`       | Domino-based board coverings using the same event pipeline.             |
| `HilbertCurve`       | Space-filling curve; recursive order controls resolution.               |
| `KochCurve`          | Classic snowflake edge recursion (4 segments per step).                 |
| `SierpinskiTriangle` | Triangular subdivision; ternary recursion.                              |

---

##  Modes: Animated vs Parallel

* **Animated (sequential)**: preserves **temporal order** for didactics; simpler to reason about; slower for large sizes.&#x20;
* **Parallel (Fork/Join)**: divides work across cores (work-stealing); fastest for higher depths; adds coordination overhead; UI updates occur post-compute.&#x20;

---

## Further reading

* **Project report** (Catalan): *Patrons repetitius. Desenvolupament i anàlisi d’una aplicació per a la representació de fractals i geometria auto-similar.* Covers MVC/event design, concurrency, and complexity derivations.&#x20;

---


## Credits

* **Dylan Canning Garcia** and collaborators (see report). Thanks to course staff for guidance and review.

---

### Citation (if you use this in teaching/research)

> Canning Garcia, D., et al. *Fractals & Tiling Visualizer (MVC + Concurrency).* Project code and report, 2025.

