# Restaurant Rush — Design Spec

Date: 2026-10-06
Source brief: `SC2002_Restaurant_Rush_Group_Assignment.docx.pdf` (not committed)
Repository: https://github.com/respoopu/SC2002_Lab_Assignment

## 1. Goal

A Java command-line, turn-based restaurant management game built in three
cumulative stages. Each stage is a tagged, independently runnable commit
(`stage-1`, `stage-2`, `stage-3`). Marks are weighted toward design evidence
(class diagram 25, sequence diagram 20, design discussion 15) and process
evidence (20), so every structural choice below must be explainable in the
report and visible in the stage-to-stage diffs.

## 2. Decisions summary

| Topic | Decision |
|---|---|
| Git workflow | Linear `main`. Each stage built on a short-lived branch, merged by PR, then the merge commit is tagged. |
| Build | Plain `javac --release 17`, no build tool. `build.sh`, `run.sh`, `test.sh`, plus raw commands in the README for Windows users. Fully offline. |
| Architecture | Entity–Control–Boundary with polymorphic domain objects. |
| Food choice | Rotation by arrival number over the menu list. |
| Critic behaviour | Cold-food penalty: −20 satisfaction if served more than one turn after the dish became READY. |
| Combo choice | Stage 3 registers three combos on the menu; the unchanged rotation rule reaches them. |
| Testing | JUnit console-standalone jar committed in `lib/`; scripted input files in `runs/`. |

## 3. Git and delivery workflow

- Branches: `feat/stage1-core`, `feat/stage2-customers`,
  `feat/stage3-host-combo-happyhour`. Branch names never equal tag names
  (avoids ambiguous refs).
- A stage is tagged only when: it compiles from a clean checkout, all tests
  pass, and its victory and defeat scripts reproduce the documented results.
- Tags are never moved once pushed.
- README compare links take the form
  `https://github.com/respoopu/SC2002_Lab_Assignment/compare/stage-1...stage-2`.
- Stage 1 contains no Stage 2/3 code; Stage 2 contains no Stage 3 code.

## 4. Repository layout

```
src/restaurantrush/boundary/   CLI and display (no game rules)
src/restaurantrush/control/    turn engine, config, arrival schedule
src/restaurantrush/entity/     domain objects that own the rules
test/restaurantrush/...        JUnit tests mirroring src packages
lib/                           JUnit console-standalone jar
runs/                          scripted inputs: stageN-victory.txt, stageN-defeat.txt
docs/design/                   this spec
docs/diagrams/                 class and sequence diagram sources/exports
build.sh  run.sh  test.sh  README.md  .gitignore
```

## 5. Architecture (Stage 1 baseline)

### 5.1 Boundary

| Class | Responsibility |
|---|---|
| `Main` | Composes the object graph (menu, restaurant, staff, schedule, config, UI, controller) and starts the game. |
| `GameCLI` | Implements `GameUI`. Reads numbered choices from a `Scanner`, rejects non-numeric / out-of-range input and re-prompts. Never decides whether a game action is legal. |
| `StatusView` | Formats the start-of-turn state, turn log, and end-of-turn results. Dollars shown as `$x.xx`, average satisfaction to one decimal or `N/A`. |

### 5.2 Control

| Class | Responsibility |
|---|---|
| `GameController` | Owns the turn loop and the step order (Section 6). Asks `GameUI` for decisions, sends them to staff, and on rejection reports the reason and asks again without consuming the task. |
| `GameUI` (interface) | The manager as seen by the controller: render state, show messages, decide Waiter task, decide Chef task. Implemented by `GameCLI` and by a scripted test double. Keeps step order out of the UI and lets whole games run in JUnit. |
| `WaiterTask` | The Waiter decision: seat(customer, table), takeOrder(customer), serve(order), or wait. |
| `ArrivalSchedule` | Maps turn → customer factory. Base: turns 1, 4, 7, 10, 13, 16. Assigns arrival numbers #1–#6. |
| `GameConfig` | Base setting: 18 turns, 2 tables, targets (≥4 paid, ≥$45.00, average ≥60). `GameConfig.base()`. |

### 5.3 Entity

| Class | Responsibility |
|---|---|
| `Restaurant` | Holds the menu, waiting list, tables, orders and customers. `seat(customer, table)` and `placeOrder(customer, turn)` validate and return `ActionResult`. Frees tables. Answers queries used for display and auto-wait. |
| `Customer` (abstract) | Id (`C1`…), arrival number/turn, satisfaction, status, table, order, served turn, ready-to-pay turn. Abstract `lossPerTurn()`, `typeName()`. Default `priceFor(item)` = full price. `chooseItem(menu)` = `menu.itemFor(arrivalNo)`. `decay()` floors at 0. `isAwaitingService()` = WAITING, SEATED or ORDERED. |
| `RegularCustomer` | `lossPerTurn()` = 8. |
| `CustomerStatus` | WAITING, SEATED, ORDERED, SERVED (eating), READY_TO_PAY, PAID, LEFT. |
| `Order` | Customer, item, locked price, units done, status, placed/ready/served turns. `canCook()`, `cook(turn)`, `canServe(turn)`, `markServed(turn)`, `markPaid()`, `cancel()`. |
| `OrderStatus` | PLACED (in preparation), READY, SERVED, PAID, CANCELLED. |
| `Menu` | Ordered list of `MenuItem`. `itemFor(arrivalNo)` = `items.get((arrivalNo − 1) % size)`. |
| `MenuItem` (abstract) | `name()`, `price()`, `prepUnits()`. |
| `Dish` | Concrete item: Salad $9.00/1, Burger $15.00/1, Pasta $18.00/2. |
| `Table` | Number and occupant. |
| `Staff` (abstract) | Name; one-task-per-turn tracking: `startTurn()`, guarded `requireAvailable()`, `markActed()`. |
| `Waiter` | `seat`, `takeOrder`, `serve`; each checks availability, delegates validation to the domain, performs, marks acted. |
| `Chef` | `cook(order, turn)`: one preparation unit. |
| `Cashier` | `collectPayment(...)`: automatic, at most one customer per turn. |
| `ActionResult` | Success message, or failure with a human-readable reason. |
| `Money` | Value object over integer cents. `plus`, `percentOff(pct)` with half-up rounding to the cent, `toString()` → `$x.xx`. |
| `Scoreboard` | Revenue, paid count, sum of satisfaction recorded at payment, served count, unhappy departures. `average()`, `meetsTargets(config)` using integer comparison `sum ≥ 60 × paidCount`. |
| `TurnLog` | Event lines written by control/entity objects during a turn (orders placed, payments, departures, reactions); drained and printed by `StatusView`. |

Stage 1 deliberately has no `onServed` hook and no pricing policy; those
arrive with the stage that needs them.

## 6. Turn engine

`GameController.playTurn(t)`:

| Step | Behaviour |
|---|---|
| 0 Reset | `startTurn()` on every staff member. |
| 1 Arrivals | Scheduled customer joins the waiting list at 100. Arrival line printed, then the full start-of-turn state (turn, revenue, average, queue, tables, each customer's satisfaction, order status, staff tasks available). |
| 2a Waiter | Decide and execute Seat / Take order / Serve / Wait. Take order prints the chosen dish and locked price. |
| 2b Chef | Decide and execute Cook one unit / Wait. Orders taken in 2a are eligible. Completing the last unit sets READY with `readyTurn = t`. |
| 3 Payment | Cashier pays the oldest READY_TO_PAY customer with `readyToPayTurn < t` (oldest = earliest `readyToPayTurn`, tie → lowest arrival number). Revenue += locked price, satisfaction recorded, customer and order PAID, table freed. |
| 4 Eating | Every SERVED customer with `servedTurn < t` becomes READY_TO_PAY with `readyToPayTurn = t`. |
| 5 Decay | Every customer with `isAwaitingService()` loses `lossPerTurn()` (floor 0). At 0: status LEFT, unfinished order CANCELLED, table freed, removed from waiting list, departure counted. |
| 6 Results | End-of-turn results: served count, unhappy departures, revenue, average (or N/A), turns used. After turn 18: Victory iff all three targets are met, else Defeat. Always runs all 18 turns. |

Timing guards are expressed on turn stamps, not only on step order:
- Serve requires `status == READY && readyTurn < t`.
- Payment requires `readyToPayTurn < t`; payment also runs before eating.

The brief's Section 3.3 Pasta example traces exactly: 92 → 84 → 76 → 76 →
READY_TO_PAY → paid turn 6 with 76 recorded.

### 6.1 Input handling

- Each staff decision is: action type (numbered), then target(s) (numbered).
  Target lists include every candidate, including invalid ones, so domain
  rejections are reachable (e.g. "Table 2 is occupied by C1",
  "C3's Pasta is still cooking (1/2)").
- Rejected choice: reason printed, same staff member asked again, task not
  consumed.
- Non-numeric or out-of-range input: re-prompt.
- Auto-wait: if a staff member has no possible action at all, they wait
  automatically with a log line.
- End of input (scripted run exhausted): exit cleanly, reporting the turn
  reached.

## 7. Stage 2 — VIP and Critic

New files:

| Class | Overrides |
|---|---|
| `VIPCustomer` | `lossPerTurn()` = 5; `priceFor(item)` = 10% off. |
| `CriticCustomer` | `lossPerTurn()` = 12; `onServed(order, t, log)`: if `t − order.readyTurn() > 1`, satisfaction −20 (floor 0), logged "C3 (Critic): cold food! −20". |

Changed files (each explained in the README):
- `Customer`: add `onServed(order, t, log)` hook, no-op by default — the
  Critic needs a service reaction.
- `Waiter.serve()`: call `customer.onServed(...)` after marking served.
- `ArrivalSchedule`: registration change — arrival #2 (turn 4) is a VIP,
  arrival #3 (turn 7) is a Critic. Still six arrivals.

Unchanged: `GameController`, `Restaurant`, `Order`, `Chef`, `Cashier`,
`GameCLI`. No `instanceof` or type switches anywhere in control code.

Design intent: the Critic orders Pasta (rotation), which becomes servable on
turn 10 — the same turn C4 arrives. Serving the Critic first records 64;
seating C4 first serves the Critic cold on turn 11 and records 32.

## 8. Stage 3 — Host, ComboMeal, Happy Hour

New files:

| Class | Role |
|---|---|
| `Host extends Staff` | Seats at most one queued customer per turn via `Restaurant.seat()` (same rules as the Waiter's seat). |
| `ComboMeal extends MenuItem` | Composed of two `MenuItem`s. `prepUnits()` = sum; `price()` = sum `percentOff(10)`. |
| `HappyHour` | One activation per game. `activate(t)` lasts turns t and t+1. `isActive()`, `turnsRemaining()`, `adjust(price)` = 20% off while active, unchanged otherwise. |

Stage 3 turn order: arrivals → display (includes Happy Hour status:
available / ACTIVE (n turns left) / used) → Happy Hour prompt if unused →
if active, every `isAwaitingService()` customer recovers +15 (cap 100) →
**Host → Waiter → Chef** → payment → eating → decay (decay set and
recovery set are the same) → results.

Pricing at order time: `price = happyHour.adjust(customer.priceFor(item))`,
locked into the `Order`. VIP first, then Happy Hour; half-up rounding at each
step. Check: VIP Burger in Happy Hour = 1500 → 1350 → 1080 cents.

Stage 3 menu (rotation order):

| # | Item | Price | Units |
|---|---|---|---|
| 1 | Salad | $9.00 | 1 |
| 2 | Burger | $15.00 | 1 |
| 3 | Pasta | $18.00 | 2 |
| 4 | Salad + Burger | $21.60 | 2 |
| 5 | Salad + Pasta | $24.30 | 3 |
| 6 | Burger + Pasta | $29.70 | 3 |

Why the Host matters: C5 (turn 13, Salad + Pasta, 3 units) can pay by turn 18
only if the Host seats them on arrival so the Waiter can take the order the
same turn.

Changed files (each explained in the README):
- `GameController`: Host step, Happy Hour prompt / recovery / countdown.
- `GameUI`, `GameCLI`: `decideHost`, `decideHappyHour`; prompts.
- `StatusView`: Happy Hour status line.
- `Restaurant`: receives the `HappyHour`; `placeOrder` wraps the price
  with `happyHour.adjust(...)`.
- `Customer`: `recover(amount)` capped at 100.
- `Main`: register combos on the menu, construct `Host` and `HappyHour`.

Alternative considered (for the report): a list of "staff decision phases"
iterated by the controller, so the Host would be registered without editing
the controller. Rejected as an extra abstraction for a single addition.

## 9. Rulings and interpretations (documented in README)

1. Arrival line and start-of-turn state are printed after step 1 so the
   manager sees the new customer before deciding.
2. The manager may seat any queued customer, not only the head of the queue.
3. Staff with no possible action wait automatically.
4. A served customer never leaves; a Critic dropped to 0 by the cold-food
   penalty still pays (abandonment applies only to unserved customers).
5. "Served count" = customers who have been served a dish.
6. Cashier "oldest first" = earliest `readyToPayTurn`, tie → lowest arrival
   number.
7. Happy Hour may be activated on turn 18; it then covers only turn 18.
8. Happy Hour recovery applies to the same customers who decay (waiting,
   seated without order, ordered but not served).
9. Discounts round half-up to the cent at each step.

## 10. Testing and reproducible runs

- Unit tests per entity rule: `Money` rounding, `Order` cook/serve guards,
  `Customer` decay floor, `Restaurant.seat` validation, Cashier ordering,
  Scoreboard targets and integer average comparison.
- Controller tests drive whole turns through a scripted `GameUI` double,
  including the brief's Pasta example (Section 6).
- Each stage adds tests; earlier-stage tests must keep passing unchanged
  unless a README-documented rule change requires otherwise.
- `runs/stageN-victory.txt` and `runs/stageN-defeat.txt` replay with
  `./run.sh < runs/stage1-victory.txt`. Each has its expected final result
  noted in the README.

Expected best-play results (base setting):

| Stage | Paid | Revenue | Satisfaction recorded | Average | Result |
|---|---|---|---|---|---|
| 1 | 5 | $66.00 | 84, 84, 76, 76, 76 | 79.2 | Victory |
| 2 | 5 | $64.50 | 84, 90, 64, 76, 76 | 78.0 | Victory |
| 3 | 5 | $86.40 | 92, 95, 100, 84, 76 | 89.4 | Victory |

Stage 3 best play: the Host seats every arrival on its arrival turn and the
Waiter takes the order that same turn; Happy Hour is activated on turn 8
(active turns 8–9) while the Critic's Pasta is cooking, so the Critic
recovers to 100 each turn and is served fresh on turn 9. No order is taken
during Happy Hour, so revenue is unaffected. C6 is seated and ordered but
cannot pay within 18 turns, as the brief intends.

## 11. Documentation the code must support

- README: build/run commands, repository URL, rulings (Section 9), per
  transition (1→2, 2→3) new files / changed files with reasons / compare
  link, scripted run instructions with expected results, video timestamps.
- Class diagram must match Stage 3 code: three packages, `Customer` and
  `Staff` hierarchies, `MenuItem`/`Dish`/`ComboMeal` composition, enums,
  `GameUI` dependency inversion.
- Suggested sequence-diagram scenario: the Stage 2 Critic, turns 7–12
  (arrival, order, Waiter/Chef assignments, READY → SERVED with the
  cold-food guard, eating, payment, result check).

## 12. Out of scope

GUI, difficulty settings, save/load, random seeds, network features, and any
extra feature that would replace a required rule.
