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

`--echo` prints each scripted answer after its prompt so the transcript reads like a live game. Lines starting with `#` are notes and are skipped. `RunScriptsTest` replays both scripts on every test run.

| Script | Strategy | Expected result |
|---|---|---|
| `victory.txt` | Seat, order and serve each customer as early as possible; serve READY dishes before seating new arrivals | VICTORY: 5 paid, $64.50, average satisfaction 78.0 |
| `defeat.txt` | Shows invalid input being rejected, then leaves the Waiter idle all game | DEFEAT: 0 paid, $0.00, average N/A, 2 unhappy departures (C1 and the Critic) |

## How to play

Every prompt is a numbered list: type the number and press Enter.

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

| Menu item | Price | Preparation units |
|---|---|---|
| Salad | $9.00 | 1 |
| Burger | $15.00 | 1 |
| Pasta | $18.00 | 2 |

**Victory:** at the end of turn 18, at least 4 paid customers, revenue of at least $45.00, and an average satisfaction of paid customers of at least 60. Otherwise, defeat.

**Food choice:** customers choose, never the manager. The rule is rotation by arrival number: customer #1 orders the first menu item, #2 the second, #3 the third, and then the cycle repeats. The choice is shown when the order is taken.

**Interpretations of the brief:**

1. The arrival message and the start-of-turn state are printed after step 1 (arrivals), so the manager sees the new customer before deciding.
2. The Waiter may seat any queued customer, not only the one at the head of the queue.
3. A staff member with no possible action waits automatically instead of being prompted.
4. "Served" in the summaries counts customers who have been served their dish, including those who later paid.
5. The Cashier's "oldest first" means the customer who became READY_TO_PAY earliest; ties go to the lower arrival number.
6. Money is stored as integer cents and shown as dollars with two decimals.
7. A served customer never leaves. A Critic whose satisfaction drops to 0 because of cold food still eats and pays; abandonment applies only to customers who have not been served.
8. Discounts are rounded half-up to the cent.

## Project structure

| Package | Role |
|---|---|
| `restaurantrush.boundary` | Console: `Main`, `GameCLI`, `StatusView`. Shows state and collects choices; no game rules. |
| `restaurantrush.control` | `GameController` (turn sequence), `GameUI` (the controller's view of the manager), `ArrivalSchedule`, `GameConfig`, `GameSetup`. |
| `restaurantrush.entity` | Domain objects that own the rules: `Restaurant`, the `Customer` hierarchy, `Order`, `Menu`/`MenuItem`/`Dish`, `Table`, the `Staff` hierarchy, `Money`, `Scoreboard`. |

## Stages

| Tag | Adds |
|---|---|
| `stage-1` | Regular customers, menu, Waiter, Chef, Cashier, the full turn sequence, victory and defeat |
| `stage-2` | VIP and Critic customers |

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
