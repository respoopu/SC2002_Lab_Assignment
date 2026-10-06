# Restaurant Rush: design discussion

This describes the final design at tag `stage-3`. The diagrams are in [`docs/diagrams/`](../diagrams/):

- a one-page class [overview](../diagrams/class-diagram-overview.svg) of every class and its structural relationships;
- four class detail pages with attributes, operations and multiplicities: [boundary](../diagrams/class-detail-1-boundary.svg), [control](../diagrams/class-detail-2-control.svg), [restaurant and staff](../diagrams/class-detail-3-restaurant-staff.svg), and [customers, orders and the menu](../diagrams/class-detail-4-customers-orders-menu.svg);
- the full [class diagram](../diagrams/class-diagram.svg) on one canvas, best viewed zoomed in;
- the [sequence diagram](../diagrams/sequence-diagram-1.svg), in ten parts.

Class names refer to `src/restaurantrush/`.

## 1. Architecture

The code is split into three packages following Entity–Control–Boundary:

- **Entity** classes hold the game state and the rules: who can be seated, when a dish can be served, what a customer pays.
- **Control** runs the turn. It asks the manager for decisions in the order the brief requires and passes each decision to the staff member who carries it out.
- **Boundary** is the console. It shows the state and reads numbered answers, but never decides whether a move is legal.

Dependencies point one way only: boundary → control → entity. The entity package imports nothing from the other two. Control reaches the console only through the `GameUI` interface, which control itself declares.

## 2. Key abstractions and their responsibilities

| Abstraction | Package | Responsibility | Works with |
|---|---|---|---|
| `GameController` | control | Plays all 18 turns and runs each turn's steps in the brief's order: arrival, Happy Hour, Host, Waiter, Chef, payment, eating, waiting. Asks `GameUI` for each decision and hands it to the right staff member. If the decision is rejected, shows the reason and asks again. Knows the order of the steps, not the rules inside them. | `GameUI`, the staff, `Restaurant`, `HappyHour`, `GameConfig` |
| `GameUI` (interface) | control | The manager as the controller sees them: show the state and messages, ask for the Happy Hour, Host, Waiter and Chef decisions, show the result. | implemented by `GameCLI` and by the test double `ScriptedUI` |
| `GameCLI` | boundary | The console version of `GameUI`: numbered menus, asking again on malformed input, skipping `#` comment lines in scripted runs. Calls read-only domain checks such as `Restaurant.canSeat()` to reject a choice early, but the rule itself stays in the domain. | `StatusView`, `Restaurant` (read-only) |
| `StatusView` | boundary | Turns the state and results into text: dollars with two decimals, the average or N/A. | `Restaurant`, `Scoreboard` |
| `GameSetup`, `GameConfig`, `ArrivalSchedule` | control | Build and configure the game. `GameSetup` assembles the base-setting game. `GameConfig` holds the 18 turns, 2 tables and three targets, and decides victory. `ArrivalSchedule` says which customer type arrives on which turn. | entity constructors |
| `WaiterTask`, `Seating` | control | A manager decision as data (seat, take an order, serve, wait), passed from the UI to the controller. | |
| `Restaurant` | entity | The dining room: tables, waiting queue, customers and orders. Holds the rules every staff member shares: seating, ordering (which locks the price) and serving. Also runs the end-of-turn eating, the Happy Hour recovery and the waiting decay. | `Customer`, `Order`, `Table`, `Menu`, `HappyHour` |
| `Staff` → `Host`, `Waiter`, `Chef`, `Cashier` | entity | `Staff.perform()` gives every role one task per turn. Each subclass offers only the actions its role is allowed: the Chef cannot seat anyone, the Host cannot serve. | `Restaurant`, `Order` |
| `Customer` → `RegularCustomer`, `VIPCustomer`, `CriticCustomer` | entity | A customer's life, WAITING → SEATED → ORDERED → SERVED → READY_TO_PAY → PAID (or LEFT), with satisfaction and the value recorded at payment. Subclasses supply only what differs: patience (`lossPerTurn`), price (`priceFor`) and the reaction to being served (`onServed`). | `Order`, `Table`, `Menu` |
| `Order` | entity | One customer's order: the item, the bill locked when the order is taken, the kitchen's progress, and the turn stamps that enforce timing (a dish is served only after the turn it became READY). Guards its own status changes. | `Customer`, `MenuItem` |
| `Menu`, `MenuItem` → `Dish`, `ComboMeal` | entity | What can be ordered. `Menu.itemFor()` is the documented food-choice rule: rotation by arrival number. A `ComboMeal` is built from two items and takes its price and preparation units from them. | `Order`, `Customer` |
| `HappyHour` | entity | The once-per-game promotion: activation, the two-turn countdown, the 20% price adjustment and its status text. | `Restaurant` |
| `Scoreboard` | entity | Revenue, paid count, average satisfaction, served count and departures, all computed from the restaurant's current state. | `Restaurant` |
| `Table`, `Money`, `ActionResult`, `TurnLog` | entity | Small supporting types. A `Table` holds one customer. `Money` is an immutable amount in integer cents. `ActionResult` is the outcome of a staff action, with the reason if it failed. `TurnLog` collects what happened this turn, so entities can report events without knowing about the console. | |

**Cohesion.** Most classes have a single reason to change:

- a pricing change touches `Money`, `HappyHour` or `VIPCustomer`;
- a change to the console layout touches `StatusView`;
- a change to the turn order touches `GameController`.

The exception is `Restaurant`, the largest class at 206 lines. It owns the rules that several staff members share, so that the Host and the Waiter seat customers through the same `seat()` method and can never disagree. We accepted its size in exchange for that guarantee (see the trade-offs in section 5).

## 3. Object-oriented concepts in the code

| Concept | Where | Why it matters here |
|---|---|---|
| Encapsulation | The methods that change a customer's, order's or table's status (`seatAt`, `attachOrder`, `markServed`, `markPaid`, `leave`, `cook`, `cancel`, `assign`, ...) and `Restaurant.seat()`, `placeOrder()` and `serve()` are package-private, and each checks its own preconditions: `Customer.requireStatus()`, `Order.canCook()` and `canServe()`, `Table.assign()`. Code outside the entity package changes a customer's, order's or table's status only through the staff actions and a few public turn steps (`admit()`, `finishEating()`, `recoverAwaitingCustomers()`, `applyWaitingDecay()`), which go through the same checked methods: the controller decides when a step happens, not how. Queries return unmodifiable lists. `Order`'s constructor is package-private, so no code outside the entity package can create an order; in the game only `Restaurant.placeOrder()` does, locking the price. `RestaurantEncapsulationTest` checks that `Restaurant.seat()`, `placeOrder()`, `serve()` and `Customer.decay()` stay non-public. | No caller can put an object into an invalid status: an illegal status change throws an exception rather than quietly corrupting the game. |
| Inheritance | The `Customer`, `Staff` and `MenuItem` hierarchies. | The customer lifecycle, the one-task-per-turn rule and the pricing interface are each written once. |
| Polymorphism | `lossPerTurn()`, `priceFor()` and `onServed()` on customers; `price()` and `prepUnits()` on menu items. Nothing in `src/` checks a customer's or menu item's type: there is no `instanceof` and no switch on a class. | New customer types and combos plug into the existing order and service flow. |
| Composition | `Restaurant` creates its tables and orders and is their only owner (UML composition); `GameController` owns its `Scoreboard` and `GameCLI` its `StatusView` in the same way. `ComboMeal` uses object composition (the Composite pattern): it is made of two `MenuItem`s and delegates to them. Its parts are shared with the menu, so the class diagram draws that link as aggregation. | No other class can add, remove or replace the restaurant's tables and orders, and a combo reuses the dishes' prices and preparation units instead of copying them. |

## 4. Design principles, where the code reflects them

### Single Responsibility (SRP)

- `Scoreboard` only computes statistics and stores nothing, so it cannot drift out of step with the restaurant.
- `StatusView` only formats text, and `GameCLI` only reads and checks input. Neither changes when a game rule changes: Stage 2 added two customer types without touching either file.
- `GameConfig` owns the victory check. It compares `sum ≥ 60 × paid` in integers, so an average of exactly 60.0 counts.
- `Money` owns rounding (half-up to the cent), so the VIP discount, Happy Hour and combo prices all round the same way.

The limit is `Restaurant`, which does several jobs (see Cohesion above).

### Open/Closed (OCP)

- **Stage 1 → 2 is the clearest evidence** ([compare](https://github.com/respoopu/SC2002_Lab_Assignment/compare/stage-1...stage-2)).
  - The VIP and Critic are two new classes (62 lines).
  - `GameController`, `Waiter`, `Chef`, `Cashier`, `Order`, `GameCLI` and `StatusView` did not change.
  - Edits to existing files were limited to:
    - a hook in `Customer` that does nothing by default (8 lines);
    - one call to that hook in `Restaurant.serve()`;
    - registering the new types in `ArrivalSchedule` and `GameSetup`.
- **`ComboMeal` in Stage 3.** `Order`, `Chef`, `Menu` and the food-choice rule did not change, because they only use `MenuItem.price()` and `prepUnits()`. A combo is one more item registered in `GameSetup.baseMenu()`, and the existing rotation rule reaches it with no code change.
- **Where the design is not closed.** The Host and Happy Hour add new steps to the turn, so `GameController` (+38/−2 lines) and `GameUI` (+7) had to change ([compare](https://github.com/respoopu/SC2002_Lab_Assignment/compare/stage-2...stage-3)). Section 6 describes a design that would have avoided this, and why we did not use it.

### Liskov Substitution (LSP)

- Code typed `Customer` works with any subtype. `Restaurant`, `Cashier` and `Scoreboard` never need to know which type they hold.
  - Subclasses change only values and reactions (`lossPerTurn`, `priceFor`, `onServed`). The lifecycle stays in `Customer`.
  - `reduceSatisfaction()` is `protected final`, so no subtype can break the rule that satisfaction stops at 0.
  - `onServed()` may lower the Critic's satisfaction but never changes the customer's status, so every customer goes on to eat and pay the same way. This is why the victory and defeat scripts needed no new inputs when Stage 2 changed two of the arrivals to a VIP and a Critic.
- A `ComboMeal` can stand in for a `Dish` anywhere. Its `prepUnits()` is the sum of two positive values, so the Chef's one-unit-per-turn cooking and the READY check in `Order.cook()` work unchanged.
- Every `Staff` subtype keeps the contract of `perform()`: at most one successful task per turn, and a rejected attempt does not use it up.

### Interface Segregation (ISP)

ISP is not strongly reflected, and that was a choice. `GameUI` has nine methods but only one client, `GameController`, which uses all nine. Splitting it (for example into a Host prompt and a Waiter prompt) would add interfaces with no second client, and the brief warns against creating interfaces just to satisfy a checklist. If a GUI or remote player only needed part of `GameUI`, we would split it then.

### Dependency Inversion (DIP)

`GameController`, the high-level turn policy, depends on `GameUI`, an interface declared in the control package, and not on the console. `GameCLI`, a low-level detail, implements it. We use this in two ways:

- Whole games run in JUnit through a scripted `GameUI` (`ScriptedUI`, used by `GameControllerTest`).
- A GUI could be added without changing the controller or any rule.

## 5. Trade-offs

| Decision | What we gain | What it costs |
|---|---|---|
| Rule violations return `ActionResult.fail(reason)` rather than throwing | Invalid choices are a normal part of play, not errors. The reason goes straight to the screen, and the controller asks again without using up the task. Exceptions are kept for programming errors, such as an illegal status change. | Every caller must check `success()`, and forgetting would silently ignore a rejection. The controller does this in one place: its retry loops. |
| Shared rules live in `Restaurant` (`seat`, `placeOrder`, `serve`) | The Host and the Waiter seat customers through the same method, so they can never disagree, and each rule is in one place. | `Restaurant` is the largest class and does several jobs. |
| Staff choose from lists that include invalid targets | The game can explain why a choice is wrong ("Table 2 is occupied by C1"), as the brief requires, instead of hiding options. | The manager sees options they cannot use. |
| Customer behaviour by subclass rather than strategy objects | Three fixed customer types are three small classes that are easy to read and test. | Behaviours cannot be mixed: a "VIP critic" would need another class. Strategy objects would allow mixing, at the cost of more classes and wiring. |
| `Scoreboard` computes statistics on demand | The statistics always match the state, and there is nothing to update in several places. | They are recomputed each time, which is trivial with six customers. |
| Food is chosen by a fixed rotation over arrival numbers | Every run is reproducible, so the scripts in `runs/` double as exact tests, and combos are reached by the same rule. | Less variety: a player who reads the rule knows every order in advance. |
| `Money` stored as integer cents | Discounts are exact and follow one rounding rule ($15 × 0.9 × 0.8 = $10.80). | Slightly more code than using `double`. |
| State-changing methods are package-private | Control and boundary code can change the state only through staff actions. | Tests that need to set up state must live in the entity package (the `Dining` test helper). |
| The UI is handed the live `Restaurant` to display it | One object to query for everything on screen; no copying of state into view objects. | The UI could in principle call a public turn step such as `applyWaitingDecay()` at the wrong time. A read-only view interface would prevent that at the cost of another interface; `RestaurantEncapsulationTest` guards the most important methods instead. |
| Events go through a `TurnLog` | Entities can report what happened ("C3 (Critic): cold food!") without depending on the console. | The log must be passed to `onServed()` and emptied by the controller after each step. |

## 6. An alternative we considered: turn phases

**The idea.** Replace the fixed steps in `GameController.playTurn()` with a list of phase objects:

```java
interface TurnPhase { void run(int turn); }

class HostPhase implements TurnPhase { /* ask chooseHostSeating(), call host.seat(), retry on failure */ }

// GameSetup registers the phases in order:
//   List.of(new ArrivalPhase(...), new HappyHourPhase(...), new HostPhase(...),
//           new WaiterPhase(...), new ChefPhase(...), new PaymentPhase(...), ...)
// and GameController just runs them:
//   for (TurnPhase phase : phases) phase.run(turn);
```

In Stage 3 the Host would then have been a new `HostPhase` registered in `GameSetup`, with no edit to the controller. This is OCP applied to the turn itself.

**Why we did not use it.**

- The brief's six steps, in a fixed order, are the core rule of the game. In `playTurn()` they read from top to bottom like the brief, with the step numbers in comments. As phases, the order would live in a list in `GameSetup`. The timing rules (the Waiter acts before the Chef; payment happens before eating) would then depend on the order in which phases were registered.
- Phases need shared state: the restaurant, the UI, the current turn and the staff. Each phase class would carry most of the controller's fields, or we would need an extra context object. That is more indirection for a single new role.
- Over the whole project the turn structure changed once, in Stage 3, by 38 added and 2 removed lines in `GameController`. The extra flexibility would only have paid off if roles were added often.

**The trade-off we accepted** is one controller edit per new staff role, documented in the README for Stage 2 → 3. If the game gained more roles or variable turn structures (for example difficulty modes), we would switch to phases.

Two other options were rejected quickly:

- **A type switch on the customer in the controller.** The brief rules this out, and Stage 2 would then have had to edit the controller.
- **Storing running totals in `Scoreboard`.** The totals could drift out of step with the restaurant's state.

## 7. How the player's choices change the result

The manager has a Host, a Waiter and a Chef, each with one task per turn, while customers lose satisfaction every turn they wait. So every turn is a choice about what to delay. Four mechanics make those choices matter:

1. **Timing rules.** A dish finished this turn can be served next turn at the earliest, and a customer who finishes eating pays the turn after that. One late decision pushes everything after it back by a turn.
2. **Customer types.** A Critic loses 12 satisfaction per waiting turn (a VIP only 5), and loses another 20 if the food sat for more than one turn after it became READY. Serving the Critic quickly gains more than serving anyone else.
3. **The Host.** A second person who can seat customers lets the Waiter take an order on the arrival turn. Without that, the turn-13 customer's 3-unit combo cannot be paid for within 18 turns.
4. **Happy Hour timing.** Switched on while customers are waiting for food, it restores their satisfaction. Switched on while orders are being taken, it also cuts those bills by 20%. When to use it is a real decision.

Each run below is a script in `runs/`, and `RunScriptsTest` replays all of them:

| Script (`runs/`) | What the manager does differently from `victory.txt` | What changes | Result |
|---|---|---|---|
| `victory.txt` | Best play: the Host seats every arrival so the Waiter can take the order the same turn; Happy Hour on turn 8; READY dishes are served before anything else. | — | **VICTORY**: 5 paid, $86.40, average 89.4 |
| `what-if-no-happy-hour.txt` | Never switches Happy Hour on. | The Critic gets no satisfaction back while the Pasta cooks and pays with 76 instead of 100. | VICTORY: 5 paid, $86.40, average 84.6 |
| `what-if-critic-served-late.txt` | On turn 9 the Waiter waits instead of serving the Critic. | The Critic waits one more turn (−12) and gets cold food (−20), paying with 68. The Waiter's lost turn also delays C4, who pays with 68 instead of 84: on turn 13 the Waiter can serve C4 or take C5's order, but not both. | VICTORY: 5 paid, $86.40, average 79.8 |
| `what-if-host-idle.txt` | On turn 13 the Host waits, and the Waiter seats C5. | C5's 3-unit order is taken a turn late, so C5 is ready to pay only on turn 18 and cannot pay. | VICTORY by the narrowest margin: 4 paid (target 4), $62.10. One more lost payment would mean defeat. |
| `defeat.txt` | The staff always wait. | Nobody is served, and C1 and the Critic run out of patience and leave. | **DEFEAT**: 0 paid, $0.00, average N/A |

The sequence diagram follows turns 7–11 of the victory run. It draws the two choices where these outcomes branch as fragments: switching on Happy Hour on turn 8 (both answers), and serving the Critic on turn 9 (fresh or cold). Its result check notes what each choice does to the final score.
