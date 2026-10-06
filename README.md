# Restaurant Rush

SC2002 group assignment: a turn-based restaurant management game for the command line, written in Java.

Repository: https://github.com/respoopu/SC2002_Lab_Assignment

You are the manager. Customers arrive, wait, order, eat and pay, and each turn you decide what your staff do. Staff time is limited, so every turn is a choice about who comes first. The game always runs 18 turns; meet all three targets to win.

## Requirements

- JDK 17 or newer (`java -version`).
- Nothing else: no build tool and no network access. The JUnit jar used by the tests is in `lib/`.

## Build and run

macOS / Linux, or Git Bash on Windows:

```bash
./build.sh   # compile into out/main
./run.sh     # build, then play
./test.sh    # build, then run every JUnit test
```

Any OS, plain commands from the project root:

```bash
javac --release 17 -d out/main -sourcepath src src/restaurantrush/boundary/Main.java
java -cp out/main restaurantrush.boundary.Main
```

## Reproducible runs

`runs/` holds scripted inputs for the base setting. Replay one with:

```bash
./run.sh --echo < runs/victory.txt
./run.sh --echo < runs/defeat.txt
```

`--echo` prints each scripted answer after its prompt so the transcript reads like a live game. Lines starting with `#` are notes and are skipped. `RunScriptsTest` replays every script below on every test run.

| Script | Strategy | Expected result |
|---|---|---|
| `victory.txt` | Host seats every arrival so the Waiter can take the order the same turn; Happy Hour on turn 8 keeps the Critic happy while the Pasta finishes; serve READY dishes before anything else | VICTORY: 5 paid, $86.40, average satisfaction 89.4 |
| `defeat.txt` | Shows invalid input being rejected, then nobody acts all game | DEFEAT: 0 paid, $0.00, average N/A, 2 unhappy departures (C1 and the Critic) |

Three what-if runs change one decision in `victory.txt` to show how a choice changes the result (see the [design discussion](docs/report/design-discussion.md#7-how-the-players-choices-change-the-result)):

| Script | Change from `victory.txt` | Expected result |
|---|---|---|
| `what-if-no-happy-hour.txt` | Happy Hour is never switched on | VICTORY: 5 paid, $86.40, average 84.6 (the Critic pays with 76 instead of 100) |
| `what-if-critic-served-late.txt` | Turn 9: the Waiter waits instead of serving the Critic | VICTORY: 5 paid, $86.40, average 79.8 (cold food: the Critic pays with 68, and C4 is delayed) |
| `what-if-host-idle.txt` | Turn 13: the Host waits and the Waiter seats C5 | VICTORY: 4 paid, $62.10, average 92.8 (C5 cannot pay in time) |

## How to play

Every prompt is a numbered list: type the number and press Enter.

- **Happy Hour** (once per game): at the start of a turn you may switch it on. It lasts that turn and the next.
- **Host** (one task per turn): seat a waiting customer at a free table, in addition to the Waiter's task. `0` waits.
- **Waiter** (one task per turn): seat a waiting customer at a free table, take a seated customer's order, or serve a READY dish. `0` waits.
- **Chef** (one preparation unit per turn): choose an order to advance. `0` waits.
- **Cashier**: automatic. Takes one payment per turn, oldest first.

If a choice breaks a rule (an occupied table, a dish that is still cooking), the game says why and asks again; the staff member's task is not used up. A staff member with nothing they could do waits automatically.

## Rules as implemented

Base setting: 18 turns, 2 tables, one Waiter, Chef and Cashier, and one customer arriving at the start of turns 1, 4, 7, 10, 13 and 16. Arrival #2 (turn 4) is a VIP and arrival #3 (turn 7) is a Critic; the others are Regular.

| Customer | Satisfaction lost per waiting turn | Bill | Special behaviour |
|---|---|---|---|
| Regular | 8 | Full price | — |
| VIP | 5 | 10% off | — |
| Critic | 12 | Full price | Cold-food penalty: if the dish is served more than one turn after it became READY, the Critic loses 20 satisfaction |

| Menu item (rotation order) | Price | Preparation units |
|---|---|---|
| Salad | $9.00 | 1 |
| Burger | $15.00 | 1 |
| Pasta | $18.00 | 2 |
| Salad + Burger combo | $21.60 | 2 |
| Salad + Pasta combo | $24.30 | 3 |
| Burger + Pasta combo | $29.70 | 3 |

A combo's preparation units are the sum of its two items, and its price is 90% of their combined price, rounded to the cent.

**Victory:** at the end of turn 18, at least 4 paid customers, revenue of at least $45.00, and an average satisfaction of paid customers of at least 60. Otherwise, defeat.

**Food choice:** customers choose, never the manager. The rule is rotation by arrival number over the menu above: customer #1 orders the first item, #2 the second, and so on, so in the base setting every customer orders something different (#4 to #6 order the combos). The choice is shown when the order is taken.

**Happy Hour:** can be switched on once per game, at the start of a turn, and lasts that turn and the next; the status line shows how many turns remain. Orders taken while it is active get 20% off, applied after the VIP discount (a VIP Burger costs $15.00 x 0.9 x 0.8 = $10.80); the price is locked when the order is taken. During each active turn, customers still waiting for a table or for food recover 15 satisfaction (maximum 100).

**Host:** seats at most one queued customer per turn, before the Waiter acts, using the same seating rules as the Waiter.

**Interpretations of the brief:**

1. The arrival message and the start-of-turn state are printed after step 1 (arrivals), so the manager sees the new customer before deciding.
2. The Waiter may seat any queued customer, not only the one at the head of the queue.
3. A staff member with no possible action waits automatically instead of being prompted.
4. "Served" in the summaries counts customers who have been served their dish, including those who later paid.
5. The Cashier's "oldest first" means the customer who became READY_TO_PAY earliest; ties go to the lower arrival number.
6. Money is stored as integer cents and shown as dollars with two decimals.
7. A served customer never leaves. A Critic whose satisfaction drops to 0 because of cold food still eats and pays; abandonment applies only to customers who have not been served.
8. Discounts are rounded half-up to the cent.
9. Each turn runs: arrivals, state display, the Happy Hour question, Happy Hour recovery (if active), Host, Waiter, Chef, payment, eating, waiting and abandonment, then the turn summary.
10. Happy Hour recovery applies to the same customers who lose satisfaction at the end of the turn: waiting for a table, seated without an order, or ordered but not yet served.
11. Happy Hour may be switched on during turn 18; it then covers only turn 18.

## Project structure

| Package | Role |
|---|---|
| `restaurantrush.boundary` | Console: `Main`, `GameCLI`, `StatusView`. Shows state and collects choices; no game rules. |
| `restaurantrush.control` | `GameController` (turn sequence), `GameUI` (the controller's view of the manager), `ArrivalSchedule`, `GameConfig`, `GameSetup`. |
| `restaurantrush.entity` | Domain objects that own the rules: `Restaurant`, the `Customer` hierarchy (Regular, VIP, Critic), `Order`, `Menu`/`MenuItem` with `Dish` and `ComboMeal`, `HappyHour`, `Table`, the `Staff` hierarchy (Host, Waiter, Chef, Cashier), `Money`, `Scoreboard`. |

## Stages

| Tag | Adds |
|---|---|
| `stage-1` | Regular customers, menu, Waiter, Chef, Cashier, the full turn sequence, victory and defeat |
| `stage-2` | VIP and Critic customers |
| `stage-3` | Host, ComboMeal and Happy Hour |

## Changes between stages

### Stage 1 → Stage 2 ([compare](https://github.com/respoopu/SC2002_Lab_Assignment/compare/stage-1...stage-2))

New files:
- `entity/VIPCustomer.java`: overrides `lossPerTurn()` (5) and `priceFor()` (10% off).
- `entity/CriticCustomer.java`: overrides `lossPerTurn()` (12) and `onServed()` (cold-food penalty).
- Tests: `VIPCustomerTest`, `CriticCustomerTest`.

Changed files:
- `entity/Customer.java`: added the `onServed(order, turn, log)` hook, which does nothing by default. The Critic needs to react to service; Regular and VIP keep the default.
- `entity/Restaurant.java`: `serve()` calls `customer.onServed(...)` after marking the dish SERVED (two lines).
- `control/ArrivalSchedule.java`: registration only. Arrival #2 is a VIP and #3 a Critic.
- `control/GameSetup.java`: edition label.
- Tests: `ArrivalScheduleTest` expects the new arrival types; `RunScriptsTest` expects the Stage 2 results; new tests in `RestaurantTest` and `GameControllerTest`.
- `runs/*.txt`: comments only. The inputs are identical because every customer type follows the same service flow.

Unchanged: `GameController`, `Waiter`, `Chef`, `Cashier`, `Order`, `GameCLI`, `StatusView`. Different behaviour is dispatched through the customer objects (`lossPerTurn()`, `priceFor()`, `onServed()`), so the controller has no type checks.

### Stage 2 → Stage 3 ([compare](https://github.com/respoopu/SC2002_Lab_Assignment/compare/stage-2...stage-3))

New files:
- `entity/ComboMeal.java`: a `MenuItem` composed of two `MenuItem`s; price and preparation units derive from its parts.
- `entity/HappyHour.java`: the once-per-game promotion (activation, countdown, 20% price adjustment, status text).
- `entity/Host.java`: a `Staff` member that seats one customer per turn via `Restaurant.seat()`.
- `control/Seating.java`: the manager's instruction to the Host.
- Tests: `ComboMealTest`, `HappyHourTest`, `HostTest`.

Changed files:
- `entity/Customer.java`: added `recover(points)`, because Happy Hour raises satisfaction (capped at 100).
- `entity/Restaurant.java`: holds the `HappyHour`; `canSeat()` exposes the seating check so the console can reject a customer before asking for a table, and `seat()` uses the same check; `placeOrder()` applies `happyHour.adjust(...)` after the customer's own discount, which is the moment the price is locked; added `recoverAwaitingCustomers()`. The original three-argument constructor is kept (a restaurant whose Happy Hour is never used), so earlier tests are unchanged.
- `control/GameUI.java`: added `askActivateHappyHour()` and `chooseHostSeating()`; a new staff role and a new manager decision need new questions.
- `control/GameController.java`: added the Happy Hour step and the Host step before the Waiter, and the Happy Hour countdown at the end of the turn. The rest of the turn sequence is unchanged.
- `control/GameSetup.java`: registers the three combos on the menu, creates the Host and the Happy Hour, edition label.
- `boundary/GameCLI.java`: the Happy Hour and Host prompts. When seating, a customer who is not waiting for a table is rejected before a table is asked for, using the new read-only `Restaurant.canSeat()`. `boundary/StatusView.java`: the Happy Hour status line.
- Tests: `ScriptedUI` and `Games` (test helpers) support the Host and Happy Hour; `RunScriptsTest` expects the Stage 3 results; new tests in `RestaurantTest`, `GameControllerTest`, `GameCLITest`, `StatusViewTest`.
- `runs/*.txt`: new answers for the Happy Hour and Host prompts.

Unchanged: the food-choice rule (`Menu.itemFor`), all customer classes except `Customer.recover`, `Order`, `Waiter`, `Chef`, `Cashier`, `ArrivalSchedule`. Combos reach customers through the existing rotation rule with no code change, only menu registration.

Alternative considered: a list of "staff decision phases" that the controller loops over, so the Host would be registered without editing `GameController`. We kept the direct version because it is one addition and easier to read; the trade-off is one controller edit per new role.

## Design evidence

- Class diagram of the final (`stage-3`) code: a one-page [overview](docs/diagrams/class-diagram-overview.svg), four detail pages ([boundary](docs/diagrams/class-detail-1-boundary.svg), [control](docs/diagrams/class-detail-2-control.svg), [restaurant and staff](docs/diagrams/class-detail-3-restaurant-staff.svg), [customers, orders and the menu](docs/diagrams/class-detail-4-customers-orders-menu.svg)) and the [full diagram](docs/diagrams/class-diagram.svg) on one canvas.
- [Sequence diagram](docs/diagrams/sequence-diagram-1.svg) in ten parts: the Critic's visit in the victory run, from arrival to payment, and the result check.
- [Design discussion](docs/report/design-discussion.md): responsibilities, design principles, trade-offs, the alternative we considered, and how the player's choices change the result.

Sources and re-rendering instructions are in [`docs/diagrams/`](docs/diagrams/README.md).

## Video timestamps

Filled in by the group after recording.

| # | Segment | Timestamp |
|---|---|---|
| 1 | The game launching from the submitted project | |
| 2 | Meaningful manager choices, including a decision where staff are constrained | |
| 3 | A dish going READY -> SERVED -> payment | |
| 4 | A VIP or Critic behaviour | |
| 5 | A Stage 3 feature | |
| 6 | One victory and one defeat | |
| 7 | The stage tag comparisons and the diagrams | |
