# Restaurant Rush Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Restaurant Rush Java command-line game in three tagged, independently runnable stages (`stage-1`, `stage-2`, `stage-3`) on a linear `main`.

**Architecture:** Entity–Control–Boundary. `entity` objects own the rules (customers decide their own decay, bill and service reaction; orders guard their own transitions; the restaurant validates seating, ordering and serving). `control` runs the six-step turn and asks a `GameUI` interface for the manager's decisions. `boundary` is a numbered-menu CLI with no game rules. Stages 2 and 3 add subclasses plus small, documented edits.

**Tech Stack:** Java 17 language level (`javac --release 17`, any JDK ≥ 17), JUnit 5.13.4 console-standalone jar, bash scripts, git, GitHub CLI (`gh`, logged in as `respoopu`).

**Spec:** `docs/design/2026-10-06-restaurant-rush-design.md`

## Global Constraints

- Compile with `javac --release 17`. No Java 18+ syntax or APIs (no pattern-matching `switch`, no record patterns, no `List.getFirst()`).
- No runtime dependencies. Tests only: `lib/junit-platform-console-standalone-1.13.4.jar`.
- Everything works offline.
- Base setting: 18 turns; 2 tables; one arrival at the start of turns 1, 4, 7, 10, 13, 16; victory = ≥4 paid customers AND revenue ≥ $45.00 AND average satisfaction of paid customers ≥ 60.
- Menu: Salad $9.00 / 1 unit, Burger $15.00 / 1 unit, Pasta $18.00 / 2 units.
- Money is integer cents (`Money`), shown as `$x.xx`; every discount step rounds half-up to the cent.
- Packages: `restaurantrush.boundary`, `restaurantrush.control`, `restaurantrush.entity`. Boundary code contains no game rules.
- No `instanceof` or type switches on customer types anywhere in `control`.
- Stage isolation: the `stage-1` tag contains no VIP/Critic/Host/Combo/Happy Hour code; `stage-2` contains no Host/Combo/Happy Hour code.
- Git: linear `main`; branches `feat/stage1-core`, `feat/stage2-customers`, `feat/stage3-host-combo-happyhour`; annotated tags `stage-1`/`stage-2`/`stage-3` on the merge commits; never move a pushed tag.
- Every commit message ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`; every PR body ends with `🤖 Generated with [Claude Code](https://claude.com/claude-code)`.
- Pushing and opening PRs is outward-facing: confirm with the user before the first push in Task 9.

## Review Focus

1. Malformed input at any prompt (letters, blank line, `2.5`, out-of-range, padded ` 0 `) must be explained and re-asked; padded numbers are accepted — pinned by `GameCLITest.malformedInputIsExplainedAndAskedAgain` (Task 8).
2. Input ending mid-game (short script, Ctrl-D) must stop with "Input ended during turn N", not a stack trace — pinned by `MainTest.inputEndingEarlyStopsTheGameWithAMessage` (Task 8).
3. A customer who leaves while their dish is mid-cook: order CANCELLED, never cookable/servable, never in revenue, table freed — pinned by `GameControllerTest.neglectedCustomerLeavesWithoutPayingAndTheirOrderIsCancelled` (Task 7) and `OrderTest.cancelledOrderCannotBeCookedOrServed` (Task 3).
4. Average satisfaction of exactly 60 must count as met (no floating-point edge) — pinned by `GameConfigTest.averageOfExactlySixtyMeetsTheTarget` (Task 6).
5. Happy Hour activated on the final turn ends the game normally, and is never offered again once used — pinned by `GameControllerTest.happyHourOnTheLastTurnStillEndsTheGameNormally` and `happyHourIsOfferedUntilUsedAndLastsTwoTurns` (Task 15).

## Working conventions

- All commands run from the project root: `/Users/rayden/Desktop/ntu/Y1S2/SC2002/lab_assignment`.
- `./test.sh` compiles everything and runs every test. A compile error in a test that references a class you have not written yet counts as the expected "failing test".
- Test classes live in the same package as the code they test so they can use package-private methods.

## File map

**Stage 1** (`feat/stage1-core`)

| File | Responsibility |
|---|---|
| `.gitignore`, `build.sh`, `run.sh`, `test.sh`, `lib/junit-platform-console-standalone-1.13.4.jar` | Build, run and test without a build tool |
| `src/restaurantrush/entity/Money.java` | Integer-cent value object |
| `src/restaurantrush/entity/MenuItem.java`, `Dish.java`, `Menu.java` | Menu items and the rotation rule |
| `src/restaurantrush/entity/CustomerStatus.java`, `OrderStatus.java` | Lifecycle enums |
| `src/restaurantrush/entity/ActionResult.java`, `TurnLog.java` | Action outcome; event lines for the CLI |
| `src/restaurantrush/entity/Table.java`, `Customer.java`, `RegularCustomer.java`, `Order.java` | Core domain objects |
| `src/restaurantrush/entity/Restaurant.java` | Queue, tables, orders; seat / order / serve / eat / decay rules |
| `src/restaurantrush/entity/Staff.java`, `Waiter.java`, `Chef.java`, `Cashier.java` | One-task-per-turn staff |
| `src/restaurantrush/entity/Scoreboard.java` | Statistics derived from the restaurant |
| `src/restaurantrush/control/GameConfig.java`, `ArrivalSchedule.java` | Base setting and arrivals |
| `src/restaurantrush/control/GameUI.java`, `WaiterTask.java`, `GameController.java`, `GameSetup.java` | Turn engine and composition |
| `src/restaurantrush/boundary/StatusView.java`, `GameCLI.java`, `InputEndedException.java`, `Main.java` | Console |
| `runs/victory.txt`, `runs/defeat.txt`, `README.md` | Reproducible runs and documentation |
| `test/restaurantrush/...` | JUnit tests mirroring the packages |

**Stage 2** (`feat/stage2-customers`): create `VIPCustomer.java`, `CriticCustomer.java`; modify `Customer.java` (hook), `Restaurant.java` (call hook), `ArrivalSchedule.java` (registration), `GameSetup.java` (edition label), tests, runs comments, README.

**Stage 3** (`feat/stage3-host-combo-happyhour`): create `ComboMeal.java`, `HappyHour.java`, `Host.java`, `control/Seating.java`; modify `Customer.java` (recover), `Restaurant.java` (Happy Hour pricing and recovery), `GameUI.java`, `GameController.java`, `GameCLI.java`, `StatusView.java`, `GameSetup.java`, tests, runs, README.

---

# Part A — Stage 1: basic restaurant operation

### Task 1: Project scaffolding and Money

**Files:**
- Create: `.gitignore`, `build.sh`, `run.sh`, `test.sh`, `lib/junit-platform-console-standalone-1.13.4.jar`
- Create: `src/restaurantrush/entity/Money.java`
- Test: `test/restaurantrush/entity/MoneyTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `record Money(long cents)` with `Money.ZERO`, `static Money ofDollars(long)`, `Money plus(Money)`, `Money percentOff(int)`, `compareTo`, `toString()` → `"$x.xx"`. Scripts `./build.sh`, `./run.sh [--echo]`, `./test.sh`.

- [ ] **Step 1: Create the stage branch**

```bash
git checkout main
git checkout -b feat/stage1-core
```

- [ ] **Step 2: Add `.gitignore`**

```
out/
.DS_Store
*.class
```

- [ ] **Step 3: Download the JUnit jar (the only network step in the project)**

```bash
mkdir -p lib
curl -fL -o lib/junit-platform-console-standalone-1.13.4.jar \
  https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.13.4/junit-platform-console-standalone-1.13.4.jar
ls -l lib/
```

Expected: a jar of roughly 2–3 MB.

- [ ] **Step 4: Write the scripts**

`build.sh`:

```bash
#!/usr/bin/env bash
# Compiles all game sources into out/main (Java 17 language level).
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out/main
mkdir -p out/main
find src -name '*.java' > out/main-sources.txt
javac --release 17 -d out/main @out/main-sources.txt
```

`run.sh`:

```bash
#!/usr/bin/env bash
# Builds and starts the game. For a scripted run, add --echo so each answer
# is printed after its prompt:   ./run.sh --echo < runs/victory.txt
set -euo pipefail
cd "$(dirname "$0")"
./build.sh
exec java -cp out/main restaurantrush.boundary.Main "$@"
```

`test.sh`:

```bash
#!/usr/bin/env bash
# Builds the game, compiles the JUnit tests and runs all of them.
set -euo pipefail
cd "$(dirname "$0")"
JUNIT=lib/junit-platform-console-standalone-1.13.4.jar
./build.sh
rm -rf out/test
mkdir -p out/test
find test -name '*.java' > out/test-sources.txt
javac --release 17 -d out/test -cp "out/main:$JUNIT" @out/test-sources.txt
java -jar "$JUNIT" execute --class-path "out/test:out/main" --scan-class-path --disable-banner
```

```bash
chmod +x build.sh run.sh test.sh
```

- [ ] **Step 5: Write the failing test** — `test/restaurantrush/entity/MoneyTest.java`

```java
package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void formatsAsDollarsWithTwoDecimals() {
        assertEquals("$9.00", Money.ofDollars(9).toString());
        assertEquals("$24.30", new Money(2430).toString());
        assertEquals("$0.05", new Money(5).toString());
    }

    @Test
    void plusAddsAmounts() {
        assertEquals(new Money(2400), Money.ofDollars(9).plus(Money.ofDollars(15)));
    }

    @Test
    void percentOffRoundsHalfUpToTheCent() {
        assertEquals(new Money(1350), Money.ofDollars(15).percentOff(10));
        assertEquals(new Money(501), new Money(1001).percentOff(50));   // 500.5 -> 501
        assertEquals(new Money(899), new Money(999).percentOff(10));    // 899.1 -> 899
        assertEquals(Money.ZERO, Money.ofDollars(3).percentOff(100));
    }

    @Test
    void rejectsNegativeAmountsAndBadPercentages() {
        assertThrows(IllegalArgumentException.class, () -> new Money(-1));
        assertThrows(IllegalArgumentException.class, () -> Money.ofDollars(1).percentOff(101));
    }

    @Test
    void comparesByAmount() {
        assertTrue(Money.ofDollars(45).compareTo(new Money(4499)) > 0);
        assertEquals(0, Money.ofDollars(45).compareTo(new Money(4500)));
    }
}
```

- [ ] **Step 6: Run it and watch it fail**

Run: `./test.sh`
Expected: FAIL — `find: src: No such file or directory` (no sources yet).

- [ ] **Step 7: Implement** — `src/restaurantrush/entity/Money.java`

```java
package restaurantrush.entity;

/**
 * An amount of money held as integer cents, so discounts never accumulate
 * floating-point error. Displayed as dollars with two decimals.
 */
public record Money(long cents) implements Comparable<Money> {

    public static final Money ZERO = new Money(0);

    public Money {
        if (cents < 0) {
            throw new IllegalArgumentException("Money cannot be negative: " + cents);
        }
    }

    public static Money ofDollars(long dollars) {
        return new Money(dollars * 100);
    }

    public Money plus(Money other) {
        return new Money(cents + other.cents);
    }

    /** This amount reduced by {@code percent}%, rounded half-up to the cent. */
    public Money percentOff(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Percent must be between 0 and 100: " + percent);
        }
        return new Money((cents * (100 - percent) + 50) / 100);
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(cents, other.cents);
    }

    @Override
    public String toString() {
        return String.format("$%d.%02d", cents / 100, cents % 100);
    }
}
```

- [ ] **Step 8: Run the tests**

Run: `./test.sh`
Expected: PASS — JUnit summary shows 5 tests successful, 0 failed.

- [ ] **Step 9: Commit**

```bash
git add .gitignore build.sh run.sh test.sh lib src test
git commit -m "$(cat <<'EOF'
Add build scripts, JUnit jar and Money value object

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

### Task 2: Menu items and the rotation rule

**Files:**
- Create: `src/restaurantrush/entity/MenuItem.java`, `Dish.java`, `Menu.java`
- Test: `test/restaurantrush/entity/MenuTest.java`

**Interfaces:**
- Consumes: `Money` (Task 1).
- Produces: `abstract class MenuItem` (`String name()`, `abstract Money price()`, `abstract int prepUnits()`, `toString()` → `"Pasta ($18.00, 2 units)"`); `Dish(String name, Money price, int prepUnits)`; `Menu(List<MenuItem>)` with `List<MenuItem> items()` and `MenuItem itemFor(int arrivalNo)`.

- [ ] **Step 1: Write the failing test** — `test/restaurantrush/entity/MenuTest.java`

```java
package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class MenuTest {
    private final Dish salad = new Dish("Salad", Money.ofDollars(9), 1);
    private final Dish burger = new Dish("Burger", Money.ofDollars(15), 1);
    private final Dish pasta = new Dish("Pasta", Money.ofDollars(18), 2);
    private final Menu menu = new Menu(List.of(salad, burger, pasta));

    @Test
    void rotationFollowsArrivalOrderAndWrapsAround() {
        assertSame(salad, menu.itemFor(1));
        assertSame(burger, menu.itemFor(2));
        assertSame(pasta, menu.itemFor(3));
        assertSame(salad, menu.itemFor(4));
        assertSame(burger, menu.itemFor(5));
        assertSame(pasta, menu.itemFor(6));
    }

    @Test
    void arrivalNumbersStartAtOne() {
        assertThrows(IllegalArgumentException.class, () -> menu.itemFor(0));
    }

    @Test
    void menuNeedsAtLeastOneItem() {
        assertThrows(IllegalArgumentException.class, () -> new Menu(List.of()));
    }

    @Test
    void dishDescribesItself() {
        assertEquals("Pasta ($18.00, 2 units)", pasta.toString());
        assertEquals("Salad ($9.00, 1 unit)", salad.toString());
    }

    @Test
    void dishNeedsAtLeastOnePreparationUnit() {
        assertThrows(IllegalArgumentException.class, () -> new Dish("Air", Money.ZERO, 0));
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `./test.sh`
Expected: FAIL — `cannot find symbol: class Dish` / `class Menu`.

- [ ] **Step 3: Implement**

`src/restaurantrush/entity/MenuItem.java`:

```java
package restaurantrush.entity;

/** Something a customer can order. */
public abstract class MenuItem {
    private final String name;

    protected MenuItem(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public abstract Money price();

    public abstract int prepUnits();

    @Override
    public String toString() {
        return name + " (" + price() + ", " + prepUnits() + (prepUnits() == 1 ? " unit)" : " units)");
    }
}
```

`src/restaurantrush/entity/Dish.java`:

```java
package restaurantrush.entity;

/** A single dish with a fixed price and preparation time. */
public class Dish extends MenuItem {
    private final Money price;
    private final int prepUnits;

    public Dish(String name, Money price, int prepUnits) {
        super(name);
        if (prepUnits < 1) {
            throw new IllegalArgumentException("A dish needs at least one preparation unit");
        }
        this.price = price;
        this.prepUnits = prepUnits;
    }

    @Override
    public Money price() {
        return price;
    }

    @Override
    public int prepUnits() {
        return prepUnits;
    }
}
```

`src/restaurantrush/entity/Menu.java`:

```java
package restaurantrush.entity;

import java.util.List;

/**
 * The items customers can order, in rotation order. Customers choose by
 * arrival number: arrival #1 orders the first item, #2 the second, and so on,
 * wrapping around when the list runs out.
 */
public class Menu {
    private final List<MenuItem> items;

    public Menu(List<MenuItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("A menu needs at least one item");
        }
        this.items = List.copyOf(items);
    }

    public List<MenuItem> items() {
        return items;
    }

    public MenuItem itemFor(int arrivalNo) {
        if (arrivalNo < 1) {
            throw new IllegalArgumentException("Arrival numbers start at 1: " + arrivalNo);
        }
        return items.get((arrivalNo - 1) % items.size());
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `./test.sh`
Expected: PASS — 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src test
git commit -m "$(cat <<'EOF'
Add menu items and the rotation food-choice rule

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

### Task 3: Customers, tables and orders

**Files:**
- Create: `src/restaurantrush/entity/CustomerStatus.java`, `OrderStatus.java`, `ActionResult.java`, `TurnLog.java`, `Table.java`, `Customer.java`, `RegularCustomer.java`, `Order.java`
- Test: `test/restaurantrush/entity/CustomerTest.java`, `test/restaurantrush/entity/OrderTest.java`

**Interfaces:**
- Consumes: `Money`, `MenuItem`, `Menu`, `Dish` (Tasks 1–2).
- Produces:
  - `enum CustomerStatus { WAITING, SEATED, ORDERED, SERVED, READY_TO_PAY, PAID, LEFT }`
  - `enum OrderStatus { PLACED, READY, SERVED, PAID, CANCELLED }`
  - `record ActionResult(boolean success, String message)` with `ok(String)`, `fail(String)`
  - `TurnLog`: `void add(String)`, `List<String> drain()`
  - `Table(int number)`: `number()`, `isFree()`, `occupant()`; package-private `assign(Customer)`, `release()`
  - `abstract Customer(int arrivalNo, int arrivalTurn)`: `abstract int lossPerTurn()`, `abstract String typeName()`, `Money priceFor(MenuItem)`, `MenuItem chooseItem(Menu)`, `id()`, `arrivalNo()`, `arrivalTurn()`, `satisfaction()`, `status()`, `table()`, `order()`, `servedTurn()`, `readyToPayTurn()`, `paidSatisfaction()`, `isAwaitingService()`, `boolean decay()`, `protected final void reduceSatisfaction(int)`; package-private `seatAt(Table)`, `attachOrder(Order)`, `markServed(int)`, `markReadyToPay(int)`, `markPaid()`, `leave()`
  - `RegularCustomer(int arrivalNo, int arrivalTurn)` — loses 8 per turn
  - `Order`: package-private constructor `Order(Customer, MenuItem, Money price, int placedTurn)`; `customer()`, `item()`, `price()`, `placedTurn()`, `unitsDone()`, `status()`, `readyTurn()`, `servedTurn()`, `progress()` → `"1/2"`, `describe()` → `"C3's Pasta"`, `ActionResult canCook()`, `ActionResult canServe(int turn)`; package-private `cook(int)`, `markServed(int)`, `markPaid()`, `cancel()`

- [ ] **Step 1: Write the failing tests**

`test/restaurantrush/entity/CustomerTest.java`:

```java
package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CustomerTest {
    private final Menu menu = new Menu(List.of(
            new Dish("Salad", Money.ofDollars(9), 1),
            new Dish("Burger", Money.ofDollars(15), 1),
            new Dish("Pasta", Money.ofDollars(18), 2)));

    @Test
    void newCustomerWaitsWithFullSatisfaction() {
        Customer customer = new RegularCustomer(3, 7);
        assertEquals("C3", customer.id());
        assertEquals("C3 (Regular)", customer.toString());
        assertEquals(7, customer.arrivalTurn());
        assertEquals(100, customer.satisfaction());
        assertEquals(CustomerStatus.WAITING, customer.status());
        assertTrue(customer.isAwaitingService());
    }

    @Test
    void regularLosesEightPerTurnAndStopsAtZero() {
        Customer customer = new RegularCustomer(1, 1);
        for (int i = 0; i < 12; i++) {
            assertFalse(customer.decay());
        }
        assertEquals(4, customer.satisfaction());
        assertTrue(customer.decay());
        assertEquals(0, customer.satisfaction());
        assertTrue(customer.decay());
        assertEquals(0, customer.satisfaction());
    }

    @Test
    void choosesFoodByArrivalRotation() {
        assertEquals("Salad", new RegularCustomer(1, 1).chooseItem(menu).name());
        assertEquals("Burger", new RegularCustomer(5, 13).chooseItem(menu).name());
        assertEquals("Pasta", new RegularCustomer(6, 16).chooseItem(menu).name());
    }

    @Test
    void regularPaysFullPrice() {
        assertEquals(Money.ofDollars(18), new RegularCustomer(3, 7).priceFor(menu.itemFor(3)));
    }
}
```

`test/restaurantrush/entity/OrderTest.java`:

```java
package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrderTest {
    private final Dish pasta = new Dish("Pasta", Money.ofDollars(18), 2);
    private final Order order = new Order(new RegularCustomer(3, 7), pasta, pasta.price(), 8);

    @Test
    void newOrderIsPlacedWithItsPriceLocked() {
        assertEquals(OrderStatus.PLACED, order.status());
        assertEquals("0/2", order.progress());
        assertEquals(Money.ofDollars(18), order.price());
        assertEquals(8, order.placedTurn());
        assertEquals("C3's Pasta", order.describe());
    }

    @Test
    void lastUnitMakesTheOrderReady() {
        order.cook(8);
        assertEquals(OrderStatus.PLACED, order.status());
        assertEquals("1/2", order.progress());
        order.cook(9);
        assertEquals(OrderStatus.READY, order.status());
        assertEquals(9, order.readyTurn());
    }

    @Test
    void readyOrderCannotBeCookedAgain() {
        order.cook(8);
        order.cook(9);
        assertEquals("C3's Pasta is already READY", order.canCook().message());
        assertThrows(IllegalStateException.class, () -> order.cook(10));
    }

    @Test
    void cannotServeWhileCooking() {
        ActionResult result = order.canServe(8);
        assertFalse(result.success());
        assertEquals("C3's Pasta is still cooking (0/2)", result.message());
    }

    @Test
    void dishReadiedThisTurnIsServedNextTurnAtTheEarliest() {
        order.cook(8);
        order.cook(9);
        assertEquals("C3's Pasta only became READY this turn; serve it next turn", order.canServe(9).message());
        assertTrue(order.canServe(10).success());
    }

    @Test
    void cancelledOrderCannotBeCookedOrServed() {
        order.cook(8);
        order.cancel();
        assertEquals(OrderStatus.CANCELLED, order.status());
        assertEquals("C3's Pasta was cancelled", order.canCook().message());
        assertFalse(order.canServe(9).success());
    }
}
```

- [ ] **Step 2: Run them and watch them fail**

Run: `./test.sh`
Expected: FAIL — `cannot find symbol: class RegularCustomer` / `class Order`.

- [ ] **Step 3: Implement the small types**

`src/restaurantrush/entity/CustomerStatus.java`:

```java
package restaurantrush.entity;

/** Where a customer is in the service flow. */
public enum CustomerStatus {
    WAITING,
    SEATED,
    ORDERED,
    SERVED,
    READY_TO_PAY,
    PAID,
    LEFT
}
```

`src/restaurantrush/entity/OrderStatus.java`:

```java
package restaurantrush.entity;

/** Where an order is between the kitchen and the Cashier. */
public enum OrderStatus {
    PLACED,
    READY,
    SERVED,
    PAID,
    CANCELLED
}
```

`src/restaurantrush/entity/ActionResult.java`:

```java
package restaurantrush.entity;

/** Outcome of a staff action: success with a description, or failure with the reason. */
public record ActionResult(boolean success, String message) {

    public static ActionResult ok(String message) {
        return new ActionResult(true, message);
    }

    public static ActionResult fail(String reason) {
        return new ActionResult(false, reason);
    }
}
```

`src/restaurantrush/entity/TurnLog.java`:

```java
package restaurantrush.entity;

import java.util.ArrayList;
import java.util.List;

/** Events that happened during the current turn, waiting to be displayed. */
public class TurnLog {
    private final List<String> lines = new ArrayList<>();

    public void add(String line) {
        lines.add(line);
    }

    /** Returns every pending line and clears the log. */
    public List<String> drain() {
        List<String> drained = List.copyOf(lines);
        lines.clear();
        return drained;
    }
}
```

`src/restaurantrush/entity/Table.java`:

```java
package restaurantrush.entity;

/** A table that holds at most one customer until they pay or leave. */
public class Table {
    private final int number;
    private Customer occupant;

    public Table(int number) {
        this.number = number;
    }

    public int number() {
        return number;
    }

    public boolean isFree() {
        return occupant == null;
    }

    /** The seated customer, or null when the table is free. */
    public Customer occupant() {
        return occupant;
    }

    void assign(Customer customer) {
        if (!isFree()) {
            throw new IllegalStateException("Table " + number + " is occupied");
        }
        occupant = customer;
    }

    void release() {
        occupant = null;
    }
}
```

- [ ] **Step 4: Implement `Customer` and `RegularCustomer`**

`src/restaurantrush/entity/Customer.java`:

```java
package restaurantrush.entity;

/**
 * A customer moving through WAITING -> SEATED -> ORDERED -> SERVED ->
 * READY_TO_PAY -> PAID, or LEFT if their satisfaction reaches 0 before they
 * are served. Subclasses decide how quickly satisfaction drops and what they
 * are charged; the lifecycle itself is the same for every customer.
 */
public abstract class Customer {
    public static final int MAX_SATISFACTION = 100;

    private final int arrivalNo;
    private final int arrivalTurn;
    private int satisfaction = MAX_SATISFACTION;
    private CustomerStatus status = CustomerStatus.WAITING;
    private Table table;
    private Order order;
    private int servedTurn;
    private int readyToPayTurn;
    private int paidSatisfaction;

    protected Customer(int arrivalNo, int arrivalTurn) {
        this.arrivalNo = arrivalNo;
        this.arrivalTurn = arrivalTurn;
    }

    /** Satisfaction lost for each turn spent waiting for a table or for food. */
    public abstract int lossPerTurn();

    /** Short label for the customer type, e.g. "Regular". */
    public abstract String typeName();

    /** What this customer is charged for an item, before any restaurant promotion. */
    public Money priceFor(MenuItem item) {
        return item.price();
    }

    /** Customers choose their own food, using the menu's rotation rule. */
    public MenuItem chooseItem(Menu menu) {
        return menu.itemFor(arrivalNo);
    }

    public String id() {
        return "C" + arrivalNo;
    }

    public int arrivalNo() {
        return arrivalNo;
    }

    public int arrivalTurn() {
        return arrivalTurn;
    }

    public int satisfaction() {
        return satisfaction;
    }

    public CustomerStatus status() {
        return status;
    }

    /** The table this customer occupies, or null if they have none. */
    public Table table() {
        return table;
    }

    /** This customer's order, or null before they order. */
    public Order order() {
        return order;
    }

    /** The turn this customer was served, or 0 if not yet served. */
    public int servedTurn() {
        return servedTurn;
    }

    /** The turn this customer finished eating, or 0 if they have not. */
    public int readyToPayTurn() {
        return readyToPayTurn;
    }

    /** Satisfaction recorded when this customer paid, or 0 if they have not paid. */
    public int paidSatisfaction() {
        return paidSatisfaction;
    }

    /** True while the customer is still waiting for a table or for food. */
    public boolean isAwaitingService() {
        return status == CustomerStatus.WAITING
                || status == CustomerStatus.SEATED
                || status == CustomerStatus.ORDERED;
    }

    /** Applies one turn of waiting. Returns true if satisfaction has reached 0. */
    public boolean decay() {
        reduceSatisfaction(lossPerTurn());
        return satisfaction == 0;
    }

    protected final void reduceSatisfaction(int points) {
        satisfaction = Math.max(0, satisfaction - points);
    }

    void seatAt(Table table) {
        requireStatus(CustomerStatus.WAITING);
        table.assign(this);
        this.table = table;
        status = CustomerStatus.SEATED;
    }

    void attachOrder(Order order) {
        requireStatus(CustomerStatus.SEATED);
        this.order = order;
        status = CustomerStatus.ORDERED;
    }

    void markServed(int turn) {
        requireStatus(CustomerStatus.ORDERED);
        order.markServed(turn);
        servedTurn = turn;
        status = CustomerStatus.SERVED;
    }

    void markReadyToPay(int turn) {
        requireStatus(CustomerStatus.SERVED);
        readyToPayTurn = turn;
        status = CustomerStatus.READY_TO_PAY;
    }

    void markPaid() {
        requireStatus(CustomerStatus.READY_TO_PAY);
        order.markPaid();
        paidSatisfaction = satisfaction;
        releaseTable();
        status = CustomerStatus.PAID;
    }

    void leave() {
        if (!isAwaitingService()) {
            throw new IllegalStateException(id() + " cannot leave while " + status);
        }
        if (order != null) {
            order.cancel();
        }
        releaseTable();
        status = CustomerStatus.LEFT;
    }

    private void releaseTable() {
        if (table != null) {
            table.release();
            table = null;
        }
    }

    private void requireStatus(CustomerStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(id() + " is " + status + ", expected " + expected);
        }
    }

    @Override
    public String toString() {
        return id() + " (" + typeName() + ")";
    }
}
```

`src/restaurantrush/entity/RegularCustomer.java`:

```java
package restaurantrush.entity;

/** The standard customer: loses 8 satisfaction per waiting turn and pays full price. */
public class RegularCustomer extends Customer {
    public static final int LOSS_PER_TURN = 8;

    public RegularCustomer(int arrivalNo, int arrivalTurn) {
        super(arrivalNo, arrivalTurn);
    }

    @Override
    public int lossPerTurn() {
        return LOSS_PER_TURN;
    }

    @Override
    public String typeName() {
        return "Regular";
    }
}
```

- [ ] **Step 5: Implement `Order`** — `src/restaurantrush/entity/Order.java`

```java
package restaurantrush.entity;

/**
 * One customer's order: what they chose, the bill locked in when the order
 * was taken, and the kitchen's progress. Guards its own status changes.
 */
public class Order {
    private final Customer customer;
    private final MenuItem item;
    private final Money price;
    private final int placedTurn;
    private int unitsDone;
    private OrderStatus status = OrderStatus.PLACED;
    private int readyTurn;
    private int servedTurn;

    Order(Customer customer, MenuItem item, Money price, int placedTurn) {
        this.customer = customer;
        this.item = item;
        this.price = price;
        this.placedTurn = placedTurn;
    }

    public Customer customer() {
        return customer;
    }

    public MenuItem item() {
        return item;
    }

    /** The bill, fixed when the order was taken. */
    public Money price() {
        return price;
    }

    public int placedTurn() {
        return placedTurn;
    }

    public int unitsDone() {
        return unitsDone;
    }

    public OrderStatus status() {
        return status;
    }

    /** The turn the last preparation unit was done, or 0 if not READY yet. */
    public int readyTurn() {
        return readyTurn;
    }

    /** The turn the dish was served, or 0 if not served yet. */
    public int servedTurn() {
        return servedTurn;
    }

    public String progress() {
        return unitsDone + "/" + item.prepUnits();
    }

    public String describe() {
        return customer.id() + "'s " + item.name();
    }

    public ActionResult canCook() {
        switch (status) {
            case PLACED:
                return ActionResult.ok(describe() + " can be cooked");
            case READY:
                return ActionResult.fail(describe() + " is already READY");
            case CANCELLED:
                return ActionResult.fail(describe() + " was cancelled");
            default:
                return ActionResult.fail(describe() + " has already been served");
        }
    }

    /** A dish can be served only if it was already READY before this turn. */
    public ActionResult canServe(int turn) {
        switch (status) {
            case PLACED:
                return ActionResult.fail(describe() + " is still cooking (" + progress() + ")");
            case READY:
                return readyTurn < turn
                        ? ActionResult.ok(describe() + " can be served")
                        : ActionResult.fail(describe() + " only became READY this turn; serve it next turn");
            case CANCELLED:
                return ActionResult.fail(describe() + " was cancelled");
            default:
                return ActionResult.fail(describe() + " has already been served");
        }
    }

    void cook(int turn) {
        ActionResult check = canCook();
        if (!check.success()) {
            throw new IllegalStateException(check.message());
        }
        unitsDone++;
        if (unitsDone == item.prepUnits()) {
            status = OrderStatus.READY;
            readyTurn = turn;
        }
    }

    void markServed(int turn) {
        ActionResult check = canServe(turn);
        if (!check.success()) {
            throw new IllegalStateException(check.message());
        }
        status = OrderStatus.SERVED;
        servedTurn = turn;
    }

    void markPaid() {
        if (status != OrderStatus.SERVED) {
            throw new IllegalStateException(describe() + " cannot be paid while " + status);
        }
        status = OrderStatus.PAID;
    }

    void cancel() {
        if (status == OrderStatus.SERVED || status == OrderStatus.PAID) {
            throw new IllegalStateException(describe() + " has already been served");
        }
        status = OrderStatus.CANCELLED;
    }
}
```

- [ ] **Step 6: Run the tests**

Run: `./test.sh`
Expected: PASS — 0 failures.

- [ ] **Step 7: Commit**

```bash
git add src test
git commit -m "$(cat <<'EOF'
Add customers, tables and orders with guarded lifecycles

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

### Task 4: Restaurant — seating, ordering, serving, eating and decay

**Files:**
- Create: `src/restaurantrush/entity/Restaurant.java`
- Test: `test/restaurantrush/entity/Dining.java` (test helper), `test/restaurantrush/entity/RestaurantTest.java`

**Interfaces:**
- Consumes: everything from Task 3.
- Produces: `Restaurant(Menu menu, int tableCount, TurnLog log)` with
  - queries: `menu()`, `log()`, `tables()`, `waitingQueue()`, `customers()`, `orders()`, `Table table(int number)`, `Customer customer(String id)`, `presentCustomers()`, `activeOrders()`, `cookableOrders()`, `servableOrders(int turn)`, `seatedWithoutOrder()`, `boolean canSeatSomeone()`
  - actions: `void admit(Customer)`, `ActionResult seat(Customer, Table)`, `ActionResult placeOrder(Customer, int turn)`, `ActionResult serve(Order, int turn)`, `void finishEating(int turn)`, `List<Customer> applyWaitingDecay()`
- Test helper `Dining` (package `restaurantrush.entity`, test tree): `static Restaurant restaurant(int tables)`, `static Order seatedWithOrder(Restaurant, Customer, int turn)`, `static void cookFully(Order, int turn)`, `static void readyToPay(Restaurant, Customer, int turn)`. Later tasks reuse it.

- [ ] **Step 1: Write the test helper** — `test/restaurantrush/entity/Dining.java`

```java
package restaurantrush.entity;

import java.util.List;

/** Test helper that walks customers through the service flow quickly. */
final class Dining {
    private Dining() {
    }

    /** A restaurant with the base menu (Salad, Burger, Pasta in rotation order). */
    static Restaurant restaurant(int tables) {
        Menu menu = new Menu(List.of(
                new Dish("Salad", Money.ofDollars(9), 1),
                new Dish("Burger", Money.ofDollars(15), 1),
                new Dish("Pasta", Money.ofDollars(18), 2)));
        return new Restaurant(menu, tables, new TurnLog());
    }

    /** Admits the customer, seats them at the first free table and takes their order on {@code turn}. */
    static Order seatedWithOrder(Restaurant restaurant, Customer customer, int turn) {
        restaurant.admit(customer);
        Table free = restaurant.tables().stream().filter(Table::isFree).findFirst().orElseThrow();
        restaurant.seat(customer, free);
        restaurant.placeOrder(customer, turn);
        return customer.order();
    }

    /** Cooks every remaining unit on {@code turn}, ignoring the Chef's one-unit limit. */
    static void cookFully(Order order, int turn) {
        while (order.status() == OrderStatus.PLACED) {
            order.cook(turn);
        }
    }

    /** Ordered and cooked on {@code turn}, served on turn + 1, READY_TO_PAY on turn + 2. */
    static void readyToPay(Restaurant restaurant, Customer customer, int turn) {
        Order order = seatedWithOrder(restaurant, customer, turn);
        cookFully(order, turn);
        restaurant.serve(order, turn + 1);
        restaurant.finishEating(turn + 2);
    }
}
```

- [ ] **Step 2: Write the failing test** — `test/restaurantrush/entity/RestaurantTest.java`

```java
package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RestaurantTest {
    private final Restaurant restaurant = Dining.restaurant(2);
    private final Customer c1 = new RegularCustomer(1, 1);
    private final Customer c2 = new RegularCustomer(2, 4);

    @Test
    void arrivingCustomerJoinsTheQueueAndIsAnnounced() {
        restaurant.admit(c1);
        assertEquals(List.of(c1), restaurant.waitingQueue());
        assertEquals(List.of("C1 (Regular) arrives and joins the waiting queue (satisfaction 100)."),
                restaurant.log().drain());
    }

    @Test
    void seatsAWaitingCustomerAtAFreeTable() {
        restaurant.admit(c1);
        ActionResult result = restaurant.seat(c1, restaurant.table(1));
        assertTrue(result.success());
        assertEquals("C1 is seated at table 1.", result.message());
        assertEquals(CustomerStatus.SEATED, c1.status());
        assertSame(c1, restaurant.table(1).occupant());
        assertTrue(restaurant.waitingQueue().isEmpty());
    }

    @Test
    void cannotSeatAtAnOccupiedTable() {
        restaurant.admit(c1);
        restaurant.admit(c2);
        restaurant.seat(c1, restaurant.table(1));
        ActionResult result = restaurant.seat(c2, restaurant.table(1));
        assertFalse(result.success());
        assertEquals("Table 1 is occupied by C1", result.message());
        assertEquals(CustomerStatus.WAITING, c2.status());
        assertEquals(List.of(c2), restaurant.waitingQueue());
    }

    @Test
    void cannotSeatSomeoneWhoIsNotWaiting() {
        restaurant.admit(c1);
        restaurant.seat(c1, restaurant.table(1));
        assertEquals("C1 is not waiting for a table (SEATED)", restaurant.seat(c1, restaurant.table(2)).message());
    }

    @Test
    void managerMaySeatAnyQueuedCustomer() {
        restaurant.admit(c1);
        restaurant.admit(c2);
        assertTrue(restaurant.seat(c2, restaurant.table(1)).success());
        assertEquals(List.of(c1), restaurant.waitingQueue());
    }

    @Test
    void orderUsesTheCustomersOwnChoiceAndLocksThePrice() {
        restaurant.admit(c2);
        restaurant.seat(c2, restaurant.table(1));
        ActionResult result = restaurant.placeOrder(c2, 5);
        assertEquals("C2 orders Burger ($15.00, 1 unit) - bill $15.00.", result.message());
        Order order = c2.order();
        assertEquals("Burger", order.item().name());
        assertEquals(Money.ofDollars(15), order.price());
        assertEquals(5, order.placedTurn());
        assertEquals(OrderStatus.PLACED, order.status());
        assertEquals(CustomerStatus.ORDERED, c2.status());
    }

    @Test
    void cannotOrderBeforeBeingSeatedOrTwice() {
        restaurant.admit(c1);
        assertEquals("C1 has not been seated yet", restaurant.placeOrder(c1, 1).message());
        restaurant.seat(c1, restaurant.table(1));
        restaurant.placeOrder(c1, 2);
        assertEquals("C1 has already ordered", restaurant.placeOrder(c1, 3).message());
    }

    @Test
    void serveNeedsADishThatWasReadyBeforeThisTurn() {
        Order order = Dining.seatedWithOrder(restaurant, c1, 1);
        assertEquals("C1's Salad is still cooking (0/1)", restaurant.serve(order, 1).message());
        order.cook(2);
        assertFalse(restaurant.serve(order, 2).success());
        ActionResult result = restaurant.serve(order, 3);
        assertEquals("C1's Salad is SERVED.", result.message());
        assertEquals(CustomerStatus.SERVED, c1.status());
        assertEquals(3, c1.servedTurn());
        assertEquals(OrderStatus.SERVED, order.status());
    }

    @Test
    void customerEatsForOneFullTurnBeforeBeingReadyToPay() {
        Order order = Dining.seatedWithOrder(restaurant, c1, 1);
        Dining.cookFully(order, 1);
        restaurant.serve(order, 2);
        restaurant.finishEating(2);
        assertEquals(CustomerStatus.SERVED, c1.status());
        restaurant.finishEating(3);
        assertEquals(CustomerStatus.READY_TO_PAY, c1.status());
        assertEquals(3, c1.readyToPayTurn());
    }

    @Test
    void onlyCustomersStillAwaitingServiceLoseSatisfaction() {
        Order order = Dining.seatedWithOrder(restaurant, c1, 1);
        Dining.cookFully(order, 1);
        restaurant.serve(order, 2);
        restaurant.admit(c2);
        restaurant.applyWaitingDecay();
        assertEquals(100, c1.satisfaction());
        assertEquals(92, c2.satisfaction());
    }

    @Test
    void customerAtZeroLeavesCancelsTheUnfinishedOrderAndFreesTheTable() {
        Customer c3 = new RegularCustomer(3, 1);
        Order pasta = Dining.seatedWithOrder(restaurant, c3, 1);
        pasta.cook(1);
        for (int i = 0; i < 12; i++) {
            assertTrue(restaurant.applyWaitingDecay().isEmpty());
        }
        restaurant.log().drain();
        assertEquals(List.of(c3), restaurant.applyWaitingDecay());
        assertEquals(CustomerStatus.LEFT, c3.status());
        assertEquals(OrderStatus.CANCELLED, pasta.status());
        assertTrue(restaurant.table(1).isFree());
        assertTrue(restaurant.activeOrders().isEmpty());
        assertTrue(restaurant.presentCustomers().isEmpty());
        assertEquals(List.of("C3 lost all patience and left without paying (order cancelled)."),
                restaurant.log().drain());
    }

    @Test
    void waitingCustomerWhoLeavesIsRemovedFromTheQueue() {
        restaurant.admit(c1);
        for (int i = 0; i < 13; i++) {
            restaurant.applyWaitingDecay();
        }
        assertEquals(CustomerStatus.LEFT, c1.status());
        assertTrue(restaurant.waitingQueue().isEmpty());
        assertFalse(restaurant.canSeatSomeone());
    }
}
```

- [ ] **Step 3: Run it and watch it fail**

Run: `./test.sh`
Expected: FAIL — `cannot find symbol: class Restaurant`.

- [ ] **Step 4: Implement** — `src/restaurantrush/entity/Restaurant.java`

```java
package restaurantrush.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The dining room: menu, tables, waiting queue, every customer who has
 * arrived and every order placed. Seating, ordering and serving are
 * validated here, so every staff member who performs them follows the same
 * rules.
 */
public class Restaurant {
    private final Menu menu;
    private final TurnLog log;
    private final List<Table> tables = new ArrayList<>();
    private final List<Customer> waitingQueue = new ArrayList<>();
    private final List<Customer> customers = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();

    public Restaurant(Menu menu, int tableCount, TurnLog log) {
        if (tableCount < 1) {
            throw new IllegalArgumentException("A restaurant needs at least one table");
        }
        this.menu = menu;
        this.log = log;
        for (int number = 1; number <= tableCount; number++) {
            tables.add(new Table(number));
        }
    }

    public Menu menu() {
        return menu;
    }

    public TurnLog log() {
        return log;
    }

    public List<Table> tables() {
        return Collections.unmodifiableList(tables);
    }

    /** Customers waiting for a table, in arrival order. */
    public List<Customer> waitingQueue() {
        return Collections.unmodifiableList(waitingQueue);
    }

    /** Everyone who has arrived, in arrival order. */
    public List<Customer> customers() {
        return Collections.unmodifiableList(customers);
    }

    /** Every order placed, in the order taken. */
    public List<Order> orders() {
        return Collections.unmodifiableList(orders);
    }

    public Table table(int number) {
        if (number < 1 || number > tables.size()) {
            throw new IllegalArgumentException("No table " + number);
        }
        return tables.get(number - 1);
    }

    public Customer customer(String id) {
        return customers.stream()
                .filter(c -> c.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No customer " + id));
    }

    /** Customers still in the restaurant (not paid and not left), in arrival order. */
    public List<Customer> presentCustomers() {
        return customers.stream()
                .filter(c -> c.status() != CustomerStatus.PAID && c.status() != CustomerStatus.LEFT)
                .toList();
    }

    /** Orders still being cooked or waiting to be served, in the order taken. */
    public List<Order> activeOrders() {
        return orders.stream()
                .filter(o -> o.status() == OrderStatus.PLACED || o.status() == OrderStatus.READY)
                .toList();
    }

    public List<Order> cookableOrders() {
        return orders.stream().filter(o -> o.status() == OrderStatus.PLACED).toList();
    }

    public List<Order> servableOrders(int turn) {
        return orders.stream().filter(o -> o.canServe(turn).success()).toList();
    }

    public List<Customer> seatedWithoutOrder() {
        return customers.stream().filter(c -> c.status() == CustomerStatus.SEATED).toList();
    }

    public boolean canSeatSomeone() {
        return !waitingQueue.isEmpty() && tables.stream().anyMatch(Table::isFree);
    }

    public void admit(Customer customer) {
        customers.add(customer);
        waitingQueue.add(customer);
        log.add(customer + " arrives and joins the waiting queue (satisfaction " + customer.satisfaction() + ").");
    }

    public ActionResult seat(Customer customer, Table table) {
        if (customer.status() != CustomerStatus.WAITING) {
            return ActionResult.fail(customer.id() + " is not waiting for a table (" + customer.status() + ")");
        }
        if (!table.isFree()) {
            return ActionResult.fail("Table " + table.number() + " is occupied by " + table.occupant().id());
        }
        waitingQueue.remove(customer);
        customer.seatAt(table);
        return ActionResult.ok(customer.id() + " is seated at table " + table.number() + ".");
    }

    /** The customer chooses their dish; the bill is fixed now and never recalculated. */
    public ActionResult placeOrder(Customer customer, int turn) {
        if (customer.status() == CustomerStatus.WAITING) {
            return ActionResult.fail(customer.id() + " has not been seated yet");
        }
        if (customer.status() != CustomerStatus.SEATED) {
            return ActionResult.fail(customer.id() + " has already ordered");
        }
        MenuItem item = customer.chooseItem(menu);
        Money price = customer.priceFor(item);
        Order order = new Order(customer, item, price, turn);
        orders.add(order);
        customer.attachOrder(order);
        return ActionResult.ok(customer.id() + " orders " + item + " - bill " + price + ".");
    }

    public ActionResult serve(Order order, int turn) {
        ActionResult check = order.canServe(turn);
        if (!check.success()) {
            return check;
        }
        order.customer().markServed(turn);
        return ActionResult.ok(order.describe() + " is SERVED.");
    }

    /** Step 4: customers served on an earlier turn have finished eating. */
    public void finishEating(int turn) {
        for (Customer customer : customers) {
            if (customer.status() == CustomerStatus.SERVED && customer.servedTurn() < turn) {
                customer.markReadyToPay(turn);
                log.add(customer.id() + " finished eating and is READY_TO_PAY.");
            }
        }
    }

    /**
     * Step 5: everyone still waiting for a table or food loses satisfaction.
     * Anyone who reaches 0 leaves without paying. Returns those who left.
     */
    public List<Customer> applyWaitingDecay() {
        List<Customer> departed = new ArrayList<>();
        for (Customer customer : customers) {
            if (customer.isAwaitingService() && customer.decay()) {
                String cancelled = customer.order() == null ? "" : " (order cancelled)";
                waitingQueue.remove(customer);
                customer.leave();
                departed.add(customer);
                log.add(customer.id() + " lost all patience and left without paying" + cancelled + ".");
            }
        }
        return departed;
    }
}
```

- [ ] **Step 5: Run the tests**

Run: `./test.sh`
Expected: PASS — 0 failures.

- [ ] **Step 6: Commit**

```bash
git add src test
git commit -m "$(cat <<'EOF'
Add Restaurant with seating, ordering, serving and decay rules

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

### Task 5: Staff — Waiter, Chef and Cashier

**Files:**
- Create: `src/restaurantrush/entity/Staff.java`, `Waiter.java`, `Chef.java`, `Cashier.java`
- Test: `test/restaurantrush/entity/StaffTest.java`

**Interfaces:**
- Consumes: `Restaurant`, `Order`, `Customer`, `ActionResult` (Tasks 3–4); test helper `Dining`.
- Produces:
  - `abstract Staff(String role)`: `role()`, `hasActed()`, `startTurn()`, `protected ActionResult perform(Supplier<ActionResult>)`
  - `Waiter()`: `ActionResult seat(Restaurant, Customer, Table)`, `ActionResult takeOrder(Restaurant, Customer, int turn)`, `ActionResult serve(Restaurant, Order, int turn)`
  - `Chef()`: `ActionResult cook(Order, int turn)`
  - `Cashier()`: `Optional<Customer> collectPayment(Restaurant, int turn)`

- [ ] **Step 1: Write the failing test** — `test/restaurantrush/entity/StaffTest.java`

```java
package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class StaffTest {
    private final Restaurant restaurant = Dining.restaurant(2);

    @Test
    void waiterCompletesAtMostOneTaskPerTurn() {
        Waiter waiter = new Waiter();
        Customer c1 = new RegularCustomer(1, 1);
        Customer c2 = new RegularCustomer(2, 1);
        restaurant.admit(c1);
        restaurant.admit(c2);
        assertTrue(waiter.seat(restaurant, c1, restaurant.table(1)).success());
        ActionResult second = waiter.seat(restaurant, c2, restaurant.table(2));
        assertFalse(second.success());
        assertEquals("Waiter has already completed a task this turn", second.message());
        waiter.startTurn();
        assertTrue(waiter.seat(restaurant, c2, restaurant.table(2)).success());
    }

    @Test
    void rejectedChoiceDoesNotUseUpTheTask() {
        Waiter waiter = new Waiter();
        Customer c1 = new RegularCustomer(1, 1);
        restaurant.admit(c1);
        assertFalse(waiter.takeOrder(restaurant, c1, 1).success());
        assertFalse(waiter.hasActed());
        assertTrue(waiter.seat(restaurant, c1, restaurant.table(1)).success());
        assertTrue(waiter.hasActed());
    }

    @Test
    void chefAdvancesOneUnitPerTurn() {
        Order pasta = Dining.seatedWithOrder(restaurant, new RegularCustomer(3, 1), 1);
        Chef chef = new Chef();
        assertEquals("Chef cooks C3's Pasta (1/2).", chef.cook(pasta, 1).message());
        assertFalse(chef.cook(pasta, 1).success());
        chef.startTurn();
        assertEquals("Chef cooks C3's Pasta (2/2) - READY.", chef.cook(pasta, 2).message());
        chef.startTurn();
        assertEquals("C3's Pasta is already READY", chef.cook(pasta, 3).message());
    }

    @Test
    void cashierWaitsATurnThenChargesOnePerTurnOldestFirst() {
        Customer c1 = new RegularCustomer(1, 1);
        Customer c2 = new RegularCustomer(2, 1);
        Dining.readyToPay(restaurant, c1, 1);
        Dining.readyToPay(restaurant, c2, 1);
        Cashier cashier = new Cashier();

        assertEquals(Optional.empty(), cashier.collectPayment(restaurant, 3));
        assertEquals(Optional.of(c1), cashier.collectPayment(restaurant, 4));
        assertEquals(Optional.empty(), cashier.collectPayment(restaurant, 4));
        cashier.startTurn();
        assertEquals(Optional.of(c2), cashier.collectPayment(restaurant, 5));

        assertEquals(CustomerStatus.PAID, c1.status());
        assertEquals(OrderStatus.PAID, c1.order().status());
        assertEquals(100, c1.paidSatisfaction());
        assertTrue(restaurant.tables().stream().allMatch(Table::isFree));
    }

    @Test
    void cashierPrefersWhoeverBecameReadyEarliest() {
        Customer c1 = new RegularCustomer(1, 1);
        Customer c2 = new RegularCustomer(2, 1);
        Dining.readyToPay(restaurant, c2, 1);
        Dining.readyToPay(restaurant, c1, 2);
        assertEquals(Optional.of(c2), new Cashier().collectPayment(restaurant, 5));
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `./test.sh`
Expected: FAIL — `cannot find symbol: class Waiter` / `Chef` / `Cashier`.

- [ ] **Step 3: Implement**

`src/restaurantrush/entity/Staff.java`:

```java
package restaurantrush.entity;

import java.util.function.Supplier;

/** A staff member who can complete at most one task per turn. */
public abstract class Staff {
    private final String role;
    private boolean actedThisTurn;

    protected Staff(String role) {
        this.role = role;
    }

    public String role() {
        return role;
    }

    public boolean hasActed() {
        return actedThisTurn;
    }

    public void startTurn() {
        actedThisTurn = false;
    }

    /**
     * Runs an action if this staff member is still free this turn. The task is
     * used up only when the action succeeds, so a rejected choice can be retried.
     */
    protected ActionResult perform(Supplier<ActionResult> action) {
        if (actedThisTurn) {
            return ActionResult.fail(role + " has already completed a task this turn");
        }
        ActionResult result = action.get();
        if (result.success()) {
            actedThisTurn = true;
        }
        return result;
    }
}
```

`src/restaurantrush/entity/Waiter.java`:

```java
package restaurantrush.entity;

/** Seats a customer, takes an order or serves a READY dish: one task per turn. */
public class Waiter extends Staff {

    public Waiter() {
        super("Waiter");
    }

    public ActionResult seat(Restaurant restaurant, Customer customer, Table table) {
        return perform(() -> restaurant.seat(customer, table));
    }

    public ActionResult takeOrder(Restaurant restaurant, Customer customer, int turn) {
        return perform(() -> restaurant.placeOrder(customer, turn));
    }

    public ActionResult serve(Restaurant restaurant, Order order, int turn) {
        return perform(() -> restaurant.serve(order, turn));
    }
}
```

`src/restaurantrush/entity/Chef.java`:

```java
package restaurantrush.entity;

/** Advances one order by one preparation unit per turn. */
public class Chef extends Staff {

    public Chef() {
        super("Chef");
    }

    public ActionResult cook(Order order, int turn) {
        return perform(() -> {
            ActionResult check = order.canCook();
            if (!check.success()) {
                return check;
            }
            order.cook(turn);
            String ready = order.status() == OrderStatus.READY ? " - READY" : "";
            return ActionResult.ok("Chef cooks " + order.describe() + " (" + order.progress() + ")" + ready + ".");
        });
    }
}
```

`src/restaurantrush/entity/Cashier.java`:

```java
package restaurantrush.entity;

import java.util.Comparator;
import java.util.Optional;

/** Takes payment automatically: at most one customer per turn, oldest first. */
public class Cashier extends Staff {

    public Cashier() {
        super("Cashier");
    }

    /**
     * Charges the customer who has been READY_TO_PAY the longest (ties go to
     * the earlier arrival). A customer who became ready this turn pays next
     * turn at the earliest. Returns the customer who paid, if any.
     */
    public Optional<Customer> collectPayment(Restaurant restaurant, int turn) {
        Optional<Customer> next = restaurant.customers().stream()
                .filter(c -> c.status() == CustomerStatus.READY_TO_PAY && c.readyToPayTurn() < turn)
                .min(Comparator.comparingInt(Customer::readyToPayTurn).thenComparingInt(Customer::arrivalNo));
        if (next.isEmpty()) {
            return Optional.empty();
        }
        Customer customer = next.get();
        ActionResult result = perform(() -> {
            Money bill = customer.order().price();
            customer.markPaid();
            restaurant.log().add("Cashier collects " + bill + " from " + customer.id()
                    + " (satisfaction " + customer.paidSatisfaction() + " recorded); their table is free.");
            return ActionResult.ok(customer.id() + " paid " + bill);
        });
        return result.success() ? next : Optional.empty();
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `./test.sh`
Expected: PASS — 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src test
git commit -m "$(cat <<'EOF'
Add Waiter, Chef and Cashier with one task per turn

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

### Task 6: Scoreboard, game configuration and arrival schedule

**Files:**
- Create: `src/restaurantrush/entity/Scoreboard.java`, `src/restaurantrush/control/GameConfig.java`, `src/restaurantrush/control/ArrivalSchedule.java`
- Test: `test/restaurantrush/entity/ScoreboardTest.java`, `test/restaurantrush/control/GameConfigTest.java`, `test/restaurantrush/control/ArrivalScheduleTest.java`

**Interfaces:**
- Consumes: `Restaurant`, `Customer`, `RegularCustomer`, `Money`, `Cashier`; test helper `Dining`.
- Produces:
  - `Scoreboard(Restaurant)`: `Money revenue()`, `int paidCount()`, `int paidSatisfactionSum()`, `OptionalDouble averageSatisfaction()`, `int servedCount()`, `int unhappyDepartures()`
  - `record GameConfig(int maxTurns, int tableCount, int targetPaidCustomers, Money targetRevenue, int targetAverageSatisfaction)`: `static GameConfig base()`, `boolean paidTargetMet(int paidCount)`, `boolean revenueTargetMet(Money)`, `boolean satisfactionTargetMet(int satisfactionSum, int paidCount)`, `boolean isVictory(Scoreboard)`
  - `ArrivalSchedule()`: `ArrivalSchedule add(int turn, BiFunction<Integer, Integer, Customer> factory)`, `Optional<Customer> arrivalAt(int turn)`, `static ArrivalSchedule base()`

- [ ] **Step 1: Write the failing tests**

`test/restaurantrush/entity/ScoreboardTest.java`:

```java
package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScoreboardTest {
    private final Restaurant restaurant = Dining.restaurant(2);
    private final Scoreboard scoreboard = new Scoreboard(restaurant);

    @Test
    void startsEmptyWithNoAverage() {
        assertEquals(Money.ZERO, scoreboard.revenue());
        assertEquals(0, scoreboard.paidCount());
        assertTrue(scoreboard.averageSatisfaction().isEmpty());
        assertEquals(0, scoreboard.servedCount());
        assertEquals(0, scoreboard.unhappyDepartures());
    }

    @Test
    void revenueAndAverageCountOnlyPaidCustomers() {
        Customer c1 = new RegularCustomer(1, 1);
        Customer c2 = new RegularCustomer(2, 1);
        Dining.readyToPay(restaurant, c1, 1);
        Dining.seatedWithOrder(restaurant, c2, 1);
        new Cashier().collectPayment(restaurant, 4);

        assertEquals(Money.ofDollars(9), scoreboard.revenue());
        assertEquals(1, scoreboard.paidCount());
        assertEquals(100, scoreboard.paidSatisfactionSum());
        assertEquals(100.0, scoreboard.averageSatisfaction().getAsDouble());
        assertEquals(1, scoreboard.servedCount());
    }

    @Test
    void departuresAreCountedAndEarnNothing() {
        Dining.seatedWithOrder(restaurant, new RegularCustomer(1, 1), 1);
        for (int i = 0; i < 13; i++) {
            restaurant.applyWaitingDecay();
        }
        assertEquals(1, scoreboard.unhappyDepartures());
        assertEquals(Money.ZERO, scoreboard.revenue());
    }
}
```

`test/restaurantrush/control/GameConfigTest.java`:

```java
package restaurantrush.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import restaurantrush.entity.Money;

class GameConfigTest {
    private final GameConfig config = GameConfig.base();

    @Test
    void baseSettingMatchesTheBrief() {
        assertEquals(18, config.maxTurns());
        assertEquals(2, config.tableCount());
        assertEquals(4, config.targetPaidCustomers());
        assertEquals(Money.ofDollars(45), config.targetRevenue());
        assertEquals(60, config.targetAverageSatisfaction());
    }

    @Test
    void paidTargetNeedsAtLeastFourCustomers() {
        assertTrue(config.paidTargetMet(4));
        assertFalse(config.paidTargetMet(3));
    }

    @Test
    void revenueTargetNeedsAtLeastFortyFiveDollars() {
        assertTrue(config.revenueTargetMet(new Money(4500)));
        assertFalse(config.revenueTargetMet(new Money(4499)));
    }

    @Test
    void averageOfExactlySixtyMeetsTheTarget() {
        assertTrue(config.satisfactionTargetMet(300, 5));
        assertFalse(config.satisfactionTargetMet(299, 5));
        assertFalse(config.satisfactionTargetMet(0, 0));
    }
}
```

`test/restaurantrush/control/ArrivalScheduleTest.java`:

```java
package restaurantrush.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import restaurantrush.entity.Customer;
import restaurantrush.entity.RegularCustomer;

class ArrivalScheduleTest {

    @Test
    void baseScheduleHasSixArrivalsNumberedInOrder() {
        ArrivalSchedule schedule = ArrivalSchedule.base();
        List<Integer> turns = new ArrayList<>();
        List<String> types = new ArrayList<>();
        for (int turn = 1; turn <= 18; turn++) {
            Optional<Customer> arrival = schedule.arrivalAt(turn);
            if (arrival.isPresent()) {
                turns.add(turn);
                types.add(arrival.get().typeName());
                assertEquals(turns.size(), arrival.get().arrivalNo());
                assertEquals(turn, arrival.get().arrivalTurn());
            }
        }
        assertEquals(List.of(1, 4, 7, 10, 13, 16), turns);
        assertEquals(List.of("Regular", "Regular", "Regular", "Regular", "Regular", "Regular"), types);
    }

    @Test
    void turnsWithoutAnArrivalAreEmpty() {
        assertTrue(ArrivalSchedule.base().arrivalAt(2).isEmpty());
    }

    @Test
    void onlyOneArrivalPerTurn() {
        ArrivalSchedule schedule = new ArrivalSchedule().add(1, RegularCustomer::new);
        assertThrows(IllegalArgumentException.class, () -> schedule.add(1, RegularCustomer::new));
    }
}
```

- [ ] **Step 2: Run them and watch them fail**

Run: `./test.sh`
Expected: FAIL — `cannot find symbol: class Scoreboard` / `GameConfig` / `ArrivalSchedule`.

- [ ] **Step 3: Implement**

`src/restaurantrush/entity/Scoreboard.java`:

```java
package restaurantrush.entity;

import java.util.OptionalDouble;

/**
 * Game statistics, computed from the restaurant's current state so they can
 * never drift out of sync with it. Revenue counts only PAID orders, so unpaid
 * and abandoned orders are never included.
 */
public class Scoreboard {
    private final Restaurant restaurant;

    public Scoreboard(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public Money revenue() {
        return restaurant.orders().stream()
                .filter(o -> o.status() == OrderStatus.PAID)
                .map(Order::price)
                .reduce(Money.ZERO, Money::plus);
    }

    public int paidCount() {
        return count(CustomerStatus.PAID);
    }

    /** Sum of the satisfaction each paid customer had when they paid. */
    public int paidSatisfactionSum() {
        return restaurant.customers().stream()
                .filter(c -> c.status() == CustomerStatus.PAID)
                .mapToInt(Customer::paidSatisfaction)
                .sum();
    }

    /** Average satisfaction of paid customers, or empty if nobody has paid. */
    public OptionalDouble averageSatisfaction() {
        int paid = paidCount();
        return paid == 0 ? OptionalDouble.empty() : OptionalDouble.of((double) paidSatisfactionSum() / paid);
    }

    /** Customers who have been served their dish, including those who have since paid. */
    public int servedCount() {
        return (int) restaurant.customers().stream().filter(c -> c.servedTurn() > 0).count();
    }

    public int unhappyDepartures() {
        return count(CustomerStatus.LEFT);
    }

    private int count(CustomerStatus status) {
        return (int) restaurant.customers().stream().filter(c -> c.status() == status).count();
    }
}
```

`src/restaurantrush/control/GameConfig.java`:

```java
package restaurantrush.control;

import restaurantrush.entity.Money;
import restaurantrush.entity.Scoreboard;

/** Length of the game, number of tables and the three victory targets. */
public record GameConfig(int maxTurns, int tableCount, int targetPaidCustomers,
                         Money targetRevenue, int targetAverageSatisfaction) {

    /** The base setting every run must offer so results can be reproduced. */
    public static GameConfig base() {
        return new GameConfig(18, 2, 4, Money.ofDollars(45), 60);
    }

    public boolean paidTargetMet(int paidCount) {
        return paidCount >= targetPaidCustomers;
    }

    public boolean revenueTargetMet(Money revenue) {
        return revenue.compareTo(targetRevenue) >= 0;
    }

    /** Compares sum >= target x count in integers, so an average of exactly 60 counts. */
    public boolean satisfactionTargetMet(int satisfactionSum, int paidCount) {
        return paidCount > 0 && satisfactionSum >= (long) targetAverageSatisfaction * paidCount;
    }

    public boolean isVictory(Scoreboard scoreboard) {
        return paidTargetMet(scoreboard.paidCount())
                && revenueTargetMet(scoreboard.revenue())
                && satisfactionTargetMet(scoreboard.paidSatisfactionSum(), scoreboard.paidCount());
    }
}
```

`src/restaurantrush/control/ArrivalSchedule.java`:

```java
package restaurantrush.control;

import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;
import restaurantrush.entity.Customer;
import restaurantrush.entity.RegularCustomer;

/** Which kind of customer arrives on which turn. At most one arrival per turn. */
public class ArrivalSchedule {
    private final TreeMap<Integer, BiFunction<Integer, Integer, Customer>> arrivals = new TreeMap<>();

    /**
     * Registers a customer type for a turn. The factory receives the arrival
     * number and the turn, e.g. {@code RegularCustomer::new}.
     */
    public ArrivalSchedule add(int turn, BiFunction<Integer, Integer, Customer> factory) {
        if (arrivals.containsKey(turn)) {
            throw new IllegalArgumentException("Turn " + turn + " already has an arrival");
        }
        arrivals.put(turn, factory);
        return this;
    }

    /** Creates the customer arriving this turn, numbered by arrival order (#1, #2, ...). */
    public Optional<Customer> arrivalAt(int turn) {
        BiFunction<Integer, Integer, Customer> factory = arrivals.get(turn);
        if (factory == null) {
            return Optional.empty();
        }
        int arrivalNo = arrivals.headMap(turn).size() + 1;
        return Optional.of(factory.apply(arrivalNo, turn));
    }

    /** Base setting: one customer at the start of turns 1, 4, 7, 10, 13 and 16. */
    public static ArrivalSchedule base() {
        return new ArrivalSchedule()
                .add(1, RegularCustomer::new)
                .add(4, RegularCustomer::new)
                .add(7, RegularCustomer::new)
                .add(10, RegularCustomer::new)
                .add(13, RegularCustomer::new)
                .add(16, RegularCustomer::new);
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `./test.sh`
Expected: PASS — 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src test
git commit -m "$(cat <<'EOF'
Add scoreboard, base game configuration and arrival schedule

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

### Task 7: Turn engine — GameUI, WaiterTask and GameController

**Files:**
- Create: `src/restaurantrush/control/GameUI.java`, `WaiterTask.java`, `GameController.java`
- Test: `test/restaurantrush/control/ScriptedUI.java`, `test/restaurantrush/control/Games.java` (test helpers), `test/restaurantrush/control/GameControllerTest.java`

**Interfaces:**
- Consumes: all entity classes, `GameConfig`, `ArrivalSchedule` (Tasks 1–6).
- Produces:
  - `interface GameUI`: `void showTurnHeader(int turn, int maxTurns)`, `void showMessage(String)`, `void showState(Restaurant, Scoreboard, List<String> availableTasks)`, `WaiterTask chooseWaiterTask(Restaurant)`, `Optional<Order> chooseOrderToCook(Restaurant)`, `void showTurnEnd(int turn, int maxTurns, Scoreboard)`, `void showResult(boolean victory, Scoreboard, GameConfig)`
  - `record WaiterTask(Kind kind, Customer customer, Table table, Order order)` with `enum Kind { SEAT, TAKE_ORDER, SERVE, WAIT }` and factories `seat(Customer, Table)`, `takeOrder(Customer)`, `serve(Order)`, `waitTurn()`
  - `GameController(GameConfig, Restaurant, ArrivalSchedule, Waiter, Chef, Cashier, GameUI)`: `boolean run()`, package-private `void playTurn(int turn)`, `int currentTurn()`, `Scoreboard scoreboard()`
  - Test helpers: `ScriptedUI` (`waiterOn(turn, Function<Restaurant, WaiterTask>)`, `chefOn(turn, Function<Restaurant, Optional<Order>>)`, fields `messages`, `lastTasks`, `waiterPrompts`, `chefPrompts`, `victory`, method `saw(String)`); `Games.config(int maxTurns)`, `Games.controller(GameConfig, Restaurant, ArrivalSchedule, GameUI)`.

- [ ] **Step 1: Write the test helpers**

`test/restaurantrush/control/ScriptedUI.java`:

```java
package restaurantrush.control;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import restaurantrush.entity.Order;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Scoreboard;

/**
 * A GameUI that replays decisions registered per turn and records what was
 * shown. When no decision is registered for a prompt, the staff member waits.
 */
class ScriptedUI implements GameUI {
    private final Map<Integer, Deque<Function<Restaurant, WaiterTask>>> waiterSteps = new HashMap<>();
    private final Map<Integer, Deque<Function<Restaurant, Optional<Order>>>> chefSteps = new HashMap<>();
    final List<String> messages = new ArrayList<>();
    List<String> lastTasks = List.of();
    int turn;
    int waiterPrompts;
    int chefPrompts;
    Boolean victory;

    ScriptedUI waiterOn(int turn, Function<Restaurant, WaiterTask> step) {
        waiterSteps.computeIfAbsent(turn, t -> new ArrayDeque<>()).add(step);
        return this;
    }

    ScriptedUI chefOn(int turn, Function<Restaurant, Optional<Order>> step) {
        chefSteps.computeIfAbsent(turn, t -> new ArrayDeque<>()).add(step);
        return this;
    }

    boolean saw(String fragment) {
        return messages.stream().anyMatch(m -> m.contains(fragment));
    }

    @Override
    public void showTurnHeader(int turn, int maxTurns) {
        this.turn = turn;
    }

    @Override
    public void showMessage(String message) {
        messages.add(message);
    }

    @Override
    public void showState(Restaurant restaurant, Scoreboard scoreboard, List<String> availableTasks) {
        lastTasks = availableTasks;
    }

    @Override
    public WaiterTask chooseWaiterTask(Restaurant restaurant) {
        waiterPrompts++;
        Deque<Function<Restaurant, WaiterTask>> steps = waiterSteps.get(turn);
        return steps == null || steps.isEmpty() ? WaiterTask.waitTurn() : steps.poll().apply(restaurant);
    }

    @Override
    public Optional<Order> chooseOrderToCook(Restaurant restaurant) {
        chefPrompts++;
        Deque<Function<Restaurant, Optional<Order>>> steps = chefSteps.get(turn);
        return steps == null || steps.isEmpty() ? Optional.empty() : steps.poll().apply(restaurant);
    }

    @Override
    public void showTurnEnd(int turn, int maxTurns, Scoreboard scoreboard) {
    }

    @Override
    public void showResult(boolean victory, Scoreboard scoreboard, GameConfig config) {
        this.victory = victory;
    }
}
```

`test/restaurantrush/control/Games.java`:

```java
package restaurantrush.control;

import restaurantrush.entity.Cashier;
import restaurantrush.entity.Chef;
import restaurantrush.entity.Money;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Waiter;

/** Builds controllers for tests, with fresh staff. */
final class Games {
    private Games() {
    }

    /** Base targets and two tables, with a custom game length. */
    static GameConfig config(int maxTurns) {
        return new GameConfig(maxTurns, 2, 4, Money.ofDollars(45), 60);
    }

    static GameController controller(GameConfig config, Restaurant restaurant, ArrivalSchedule schedule, GameUI ui) {
        return new GameController(config, restaurant, schedule, new Waiter(), new Chef(), new Cashier(), ui);
    }
}
```

- [ ] **Step 2: Write the failing test** — `test/restaurantrush/control/GameControllerTest.java`

```java
package restaurantrush.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.Dish;
import restaurantrush.entity.Menu;
import restaurantrush.entity.Money;
import restaurantrush.entity.OrderStatus;
import restaurantrush.entity.RegularCustomer;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.TurnLog;

class GameControllerTest {
    private final Dish salad = new Dish("Salad", Money.ofDollars(9), 1);
    private final Dish pasta = new Dish("Pasta", Money.ofDollars(18), 2);

    private Restaurant restaurantServing(Dish dish) {
        return new Restaurant(new Menu(List.of(dish)), 2, new TurnLog());
    }

    private ArrivalSchedule oneRegularOnTurnOne() {
        return new ArrivalSchedule().add(1, RegularCustomer::new);
    }

    @Test
    void briefPastaExampleMatchesTurnByTurn() {
        Restaurant restaurant = restaurantServing(pasta);
        ScriptedUI ui = new ScriptedUI()
                .waiterOn(1, r -> WaiterTask.seat(r.customer("C1"), r.table(1)))
                .waiterOn(2, r -> WaiterTask.takeOrder(r.customer("C1")))
                .chefOn(2, r -> Optional.of(r.customer("C1").order()))
                .chefOn(3, r -> Optional.of(r.customer("C1").order()))
                .waiterOn(4, r -> WaiterTask.serve(r.customer("C1").order()));
        GameController game = Games.controller(Games.config(6), restaurant, oneRegularOnTurnOne(), ui);

        game.playTurn(1);
        Customer c1 = restaurant.customer("C1");
        assertEquals(CustomerStatus.SEATED, c1.status());
        assertEquals(92, c1.satisfaction());

        game.playTurn(2);
        assertEquals(CustomerStatus.ORDERED, c1.status());
        assertEquals("1/2", c1.order().progress());
        assertEquals(84, c1.satisfaction());

        game.playTurn(3);
        assertEquals(OrderStatus.READY, c1.order().status());
        assertEquals(76, c1.satisfaction());

        game.playTurn(4);
        assertEquals(CustomerStatus.SERVED, c1.status());
        assertEquals(76, c1.satisfaction());

        game.playTurn(5);
        assertEquals(CustomerStatus.READY_TO_PAY, c1.status());

        game.playTurn(6);
        assertEquals(CustomerStatus.PAID, c1.status());
        assertEquals(76, c1.paidSatisfaction());
        assertEquals(Money.ofDollars(18), game.scoreboard().revenue());
        assertTrue(restaurant.table(1).isFree());
    }

    @Test
    void rejectedChoiceExplainsWhyAndAsksAgainWithoutUsingTheTask() {
        Restaurant restaurant = restaurantServing(salad);
        ScriptedUI ui = new ScriptedUI()
                .waiterOn(1, r -> WaiterTask.takeOrder(r.customer("C1")))
                .waiterOn(1, r -> WaiterTask.seat(r.customer("C1"), r.table(1)));
        GameController game = Games.controller(Games.config(1), restaurant, oneRegularOnTurnOne(), ui);

        game.playTurn(1);

        assertEquals(2, ui.waiterPrompts);
        assertTrue(ui.saw("Not allowed: C1 has not been seated yet. Choose again."));
        assertEquals(CustomerStatus.SEATED, restaurant.customer("C1").status());
    }

    @Test
    void staffWithNothingToDoWaitWithoutBeingAsked() {
        ScriptedUI ui = new ScriptedUI();
        GameController game = Games.controller(Games.config(1), restaurantServing(salad), new ArrivalSchedule(), ui);

        game.playTurn(1);

        assertEquals(0, ui.waiterPrompts);
        assertEquals(0, ui.chefPrompts);
        assertTrue(ui.saw("Waiter has nothing to do this turn and waits."));
        assertTrue(ui.saw("Chef has nothing to cook and waits."));
    }

    @Test
    void startOfTurnDisplayListsTheTasksStaffCanDo() {
        ScriptedUI ui = new ScriptedUI();
        Games.controller(Games.config(1), restaurantServing(salad), oneRegularOnTurnOne(), ui).playTurn(1);

        assertTrue(ui.lastTasks.contains("Waiter: seat a waiting customer"));
        assertTrue(ui.lastTasks.stream().noneMatch(task -> task.startsWith("Chef")));
    }

    @Test
    void neglectedCustomerLeavesWithoutPayingAndTheirOrderIsCancelled() {
        Restaurant restaurant = restaurantServing(pasta);
        ScriptedUI ui = new ScriptedUI()
                .waiterOn(1, r -> WaiterTask.seat(r.customer("C1"), r.table(1)))
                .waiterOn(2, r -> WaiterTask.takeOrder(r.customer("C1")))
                .chefOn(2, r -> Optional.of(r.customer("C1").order()));
        GameController game = Games.controller(Games.config(13), restaurant, oneRegularOnTurnOne(), ui);

        boolean victory = game.run();

        Customer c1 = restaurant.customer("C1");
        assertFalse(victory);
        assertEquals(Boolean.FALSE, ui.victory);
        assertEquals(CustomerStatus.LEFT, c1.status());
        assertEquals(OrderStatus.CANCELLED, c1.order().status());
        assertTrue(restaurant.table(1).isFree());
        assertEquals(1, game.scoreboard().unhappyDepartures());
        assertEquals(Money.ZERO, game.scoreboard().revenue());
        assertTrue(ui.saw("C1 lost all patience and left without paying (order cancelled)."));
    }
}
```

- [ ] **Step 3: Run it and watch it fail**

Run: `./test.sh`
Expected: FAIL — `cannot find symbol: class GameUI` / `WaiterTask` / `GameController`.

- [ ] **Step 4: Implement**

`src/restaurantrush/control/GameUI.java`:

```java
package restaurantrush.control;

import java.util.List;
import java.util.Optional;
import restaurantrush.entity.Order;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Scoreboard;

/**
 * The manager's side of the game, as the controller sees it. The controller
 * decides when each question is asked; implementations only display state
 * and collect answers.
 */
public interface GameUI {

    void showTurnHeader(int turn, int maxTurns);

    void showMessage(String message);

    void showState(Restaurant restaurant, Scoreboard scoreboard, List<String> availableTasks);

    WaiterTask chooseWaiterTask(Restaurant restaurant);

    /** The order the Chef should advance by one unit, or empty to wait. */
    Optional<Order> chooseOrderToCook(Restaurant restaurant);

    void showTurnEnd(int turn, int maxTurns, Scoreboard scoreboard);

    void showResult(boolean victory, Scoreboard scoreboard, GameConfig config);
}
```

`src/restaurantrush/control/WaiterTask.java`:

```java
package restaurantrush.control;

import restaurantrush.entity.Customer;
import restaurantrush.entity.Order;
import restaurantrush.entity.Table;

/** The manager's instruction to the Waiter for this turn. Unused fields are null. */
public record WaiterTask(Kind kind, Customer customer, Table table, Order order) {

    public enum Kind { SEAT, TAKE_ORDER, SERVE, WAIT }

    public static WaiterTask seat(Customer customer, Table table) {
        return new WaiterTask(Kind.SEAT, customer, table, null);
    }

    public static WaiterTask takeOrder(Customer customer) {
        return new WaiterTask(Kind.TAKE_ORDER, customer, null, null);
    }

    public static WaiterTask serve(Order order) {
        return new WaiterTask(Kind.SERVE, null, null, order);
    }

    public static WaiterTask waitTurn() {
        return new WaiterTask(Kind.WAIT, null, null, null);
    }
}
```

`src/restaurantrush/control/GameController.java`:

```java
package restaurantrush.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import restaurantrush.entity.ActionResult;
import restaurantrush.entity.Cashier;
import restaurantrush.entity.Chef;
import restaurantrush.entity.Customer;
import restaurantrush.entity.Order;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Scoreboard;
import restaurantrush.entity.Waiter;

/**
 * Runs the game: every turn follows the brief's six steps in order. The
 * controller owns the step order and the "an invalid choice does not use up
 * the task" rule; the rules for each action live in the staff and domain
 * objects.
 */
public class GameController {
    private final GameConfig config;
    private final Restaurant restaurant;
    private final Scoreboard scoreboard;
    private final ArrivalSchedule schedule;
    private final Waiter waiter;
    private final Chef chef;
    private final Cashier cashier;
    private final GameUI ui;
    private int turn;

    public GameController(GameConfig config, Restaurant restaurant, ArrivalSchedule schedule,
                          Waiter waiter, Chef chef, Cashier cashier, GameUI ui) {
        this.config = config;
        this.restaurant = restaurant;
        this.scoreboard = new Scoreboard(restaurant);
        this.schedule = schedule;
        this.waiter = waiter;
        this.chef = chef;
        this.cashier = cashier;
        this.ui = ui;
    }

    public int currentTurn() {
        return turn;
    }

    public Scoreboard scoreboard() {
        return scoreboard;
    }

    /** Plays every turn (there is no early ending) and returns true on victory. */
    public boolean run() {
        for (int t = 1; t <= config.maxTurns(); t++) {
            playTurn(t);
        }
        boolean victory = config.isVictory(scoreboard);
        ui.showResult(victory, scoreboard, config);
        return victory;
    }

    /** Plays one turn. Package-private so tests can check the state between turns. */
    void playTurn(int t) {
        turn = t;
        waiter.startTurn();
        chef.startTurn();
        cashier.startTurn();

        ui.showTurnHeader(turn, config.maxTurns());
        schedule.arrivalAt(turn).ifPresent(restaurant::admit);      // 1 arrivals
        flushLog();
        ui.showState(restaurant, scoreboard, availableTasks());

        waiterStep();                                               // 2-3 the Waiter acts before the Chef
        chefStep();

        cashier.collectPayment(restaurant, turn);                   // 4 payment, then eating
        restaurant.finishEating(turn);
        restaurant.applyWaitingDecay();                             // 5 waiting and abandonment
        flushLog();
        ui.showTurnEnd(turn, config.maxTurns(), scoreboard);        // 6 results
    }

    private void waiterStep() {
        if (!waiterHasWork()) {
            ui.showMessage("Waiter has nothing to do this turn and waits.");
            return;
        }
        while (true) {
            ActionResult result = execute(ui.chooseWaiterTask(restaurant));
            report(result);
            if (result.success()) {
                return;
            }
        }
    }

    private ActionResult execute(WaiterTask task) {
        switch (task.kind()) {
            case SEAT:
                return waiter.seat(restaurant, task.customer(), task.table());
            case TAKE_ORDER:
                return waiter.takeOrder(restaurant, task.customer(), turn);
            case SERVE:
                return waiter.serve(restaurant, task.order(), turn);
            default:
                return ActionResult.ok("Waiter waits.");
        }
    }

    private void chefStep() {
        if (restaurant.cookableOrders().isEmpty()) {
            ui.showMessage("Chef has nothing to cook and waits.");
            return;
        }
        while (true) {
            Optional<Order> choice = ui.chooseOrderToCook(restaurant);
            ActionResult result = choice.map(order -> chef.cook(order, turn))
                    .orElse(ActionResult.ok("Chef waits."));
            report(result);
            if (result.success()) {
                return;
            }
        }
    }

    private void report(ActionResult result) {
        ui.showMessage(result.success() ? result.message() : "Not allowed: " + result.message() + ". Choose again.");
        flushLog();
    }

    private void flushLog() {
        restaurant.log().drain().forEach(ui::showMessage);
    }

    private boolean waiterHasWork() {
        return restaurant.canSeatSomeone()
                || !restaurant.seatedWithoutOrder().isEmpty()
                || !restaurant.servableOrders(turn).isEmpty();
    }

    private List<String> availableTasks() {
        List<String> tasks = new ArrayList<>();
        if (restaurant.canSeatSomeone()) {
            tasks.add("Waiter: seat a waiting customer");
        }
        for (Customer customer : restaurant.seatedWithoutOrder()) {
            tasks.add("Waiter: take " + customer.id() + "'s order");
        }
        for (Order order : restaurant.servableOrders(turn)) {
            tasks.add("Waiter: serve " + order.describe());
        }
        for (Order order : restaurant.cookableOrders()) {
            tasks.add("Chef: cook " + order.describe() + " (" + order.progress() + ")");
        }
        if (tasks.isEmpty()) {
            tasks.add("None - staff will wait");
        }
        return tasks;
    }
}
```

- [ ] **Step 5: Run the tests**

Run: `./test.sh`
Expected: PASS — 0 failures.

- [ ] **Step 6: Commit**

```bash
git add src test
git commit -m "$(cat <<'EOF'
Add turn engine driving staff decisions through GameUI

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

### Task 8: Console — StatusView, GameCLI, GameSetup and Main

**Files:**
- Create: `src/restaurantrush/control/GameSetup.java`, `src/restaurantrush/boundary/StatusView.java`, `GameCLI.java`, `InputEndedException.java`, `Main.java`
- Test: `test/restaurantrush/boundary/StatusViewTest.java`, `GameCLITest.java`, `MainTest.java`

**Interfaces:**
- Consumes: everything from Tasks 1–7.
- Produces:
  - `GameSetup`: `static final String EDITION`, `static Menu baseMenu()`, `static GameController baseGame(GameUI)`
  - `StatusView`: `header(int, int)`, `state(Restaurant, Scoreboard, List<String>)`, `describeCustomer(Customer)`, `describeOrder(Order)`, `describeTable(Table)`, `turnEnd(int, int, Scoreboard)`, `result(boolean, Scoreboard, GameConfig)`, `static String average(Scoreboard)`
  - `GameCLI(InputStream in, PrintStream out, boolean echoInput) implements GameUI`
  - `InputEndedException extends RuntimeException`
  - `Main`: `main(String[] args)` (flag `--echo`), package-private `static void play(InputStream, PrintStream, boolean echo)`

**Prompt layout** (scripted runs depend on it):
- Waiter: `1) Seat a waiting customer`, `2) Take an order`, `3) Serve a READY dish`, `0) Wait`. Seat → pick from `presentCustomers()` then from `tables()`. Take order → pick from `presentCustomers()`. Serve → pick from `activeOrders()`. In every pick list `0` goes back to the Waiter menu.
- Chef: pick from `activeOrders()`, `0` = Wait.
- Empty pick list → prints `There is nothing to choose from.` and goes back.
- Lines starting with `#` are skipped (notes in run files).

- [ ] **Step 1: Write the failing tests**

`test/restaurantrush/boundary/StatusViewTest.java`:

```java
package restaurantrush.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import restaurantrush.control.GameConfig;
import restaurantrush.control.GameSetup;
import restaurantrush.entity.Customer;
import restaurantrush.entity.RegularCustomer;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Scoreboard;
import restaurantrush.entity.TurnLog;

class StatusViewTest {
    private final StatusView view = new StatusView();
    private final Restaurant restaurant = new Restaurant(GameSetup.baseMenu(), 2, new TurnLog());
    private final Scoreboard scoreboard = new Scoreboard(restaurant);

    @Test
    void averageIsNotApplicableUntilSomeonePays() {
        assertEquals("N/A", StatusView.average(scoreboard));
    }

    @Test
    void stateShowsRevenueQueueTablesCustomersAndTasks() {
        restaurant.admit(new RegularCustomer(1, 1));
        String state = view.state(restaurant, scoreboard, List.of("Waiter: seat a waiting customer"));
        assertTrue(state.contains("Revenue: $0.00 | Avg satisfaction (paid): N/A | Paid customers: 0"), state);
        assertTrue(state.contains("Waiting queue: C1 (Regular, 100)"), state);
        assertTrue(state.contains("Tables: Table 1 - free | Table 2 - free"), state);
        assertTrue(state.contains("  C1 (Regular) - WAITING, satisfaction 100, no order yet"), state);
        assertTrue(state.contains("  - Waiter: seat a waiting customer"), state);
    }

    @Test
    void customerLineShowsTableAndOrderProgress() {
        Customer c3 = new RegularCustomer(3, 1);
        restaurant.admit(c3);
        restaurant.seat(c3, restaurant.table(2));
        restaurant.placeOrder(c3, 1);
        assertEquals("C3 (Regular) - ORDERED at table 2, satisfaction 100, Pasta $18.00: cooking 0/2",
                view.describeCustomer(c3));
        assertEquals("C3's Pasta ($18.00): cooking 0/2", view.describeOrder(c3.order()));
        assertEquals("Table 2 - occupied by C3", view.describeTable(restaurant.table(2)));
    }

    @Test
    void turnEndSummarisesTheTurn() {
        assertEquals("End of turn 3: served 0 | unhappy departures 0 | revenue $0.00"
                + " | avg satisfaction N/A | turns used 3/18", view.turnEnd(3, 18, scoreboard));
    }

    @Test
    void resultListsEachTarget() {
        String result = view.result(false, scoreboard, GameConfig.base());
        assertTrue(result.contains("GAME OVER: DEFEAT"), result);
        assertTrue(result.contains("Paid customers: 0 (target 4) [missed]"), result);
        assertTrue(result.contains("Revenue: $0.00 (target $45.00) [missed]"), result);
        assertTrue(result.contains("Average satisfaction: N/A (target 60) [missed]"), result);
        assertTrue(result.contains("Served: 0 | Unhappy departures: 0 | Turns used: 18/18"), result);
    }
}
```

`test/restaurantrush/boundary/GameCLITest.java`:

```java
package restaurantrush.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import restaurantrush.control.GameSetup;
import restaurantrush.control.WaiterTask;
import restaurantrush.entity.Order;
import restaurantrush.entity.RegularCustomer;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.TurnLog;

class GameCLITest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        restaurant = new Restaurant(GameSetup.baseMenu(), 2, new TurnLog());
        restaurant.admit(new RegularCustomer(1, 1));
        restaurant.admit(new RegularCustomer(2, 1));
    }

    private GameCLI cli(String input, boolean echo) {
        return new GameCLI(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8), echo);
    }

    private GameCLI cli(String input) {
        return cli(input, false);
    }

    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void seatChoiceNamesTheCustomerAndTable() {
        WaiterTask task = cli("1\n2\n1\n").chooseWaiterTask(restaurant);
        assertEquals(WaiterTask.Kind.SEAT, task.kind());
        assertEquals("C2", task.customer().id());
        assertEquals(1, task.table().number());
    }

    @Test
    void malformedInputIsExplainedAndAskedAgain() {
        WaiterTask task = cli("abc\n\n2.5\n7\n 0 \n").chooseWaiterTask(restaurant);
        assertEquals(WaiterTask.Kind.WAIT, task.kind());
        String out = printed();
        assertTrue(out.contains("Invalid input 'abc': enter a number from 0 to 3."), out);
        assertTrue(out.contains("Invalid input '': enter a number from 0 to 3."), out);
        assertTrue(out.contains("Invalid input '2.5'"), out);
        assertTrue(out.contains("Invalid input '7'"), out);
    }

    @Test
    void zeroInATargetListGoesBackToTheTaskMenu() {
        assertEquals(WaiterTask.Kind.WAIT, cli("2\n0\n0\n").chooseWaiterTask(restaurant).kind());
    }

    @Test
    void emptyTargetListSaysSoAndGoesBack() {
        assertEquals(WaiterTask.Kind.WAIT, cli("3\n0\n").chooseWaiterTask(restaurant).kind());
        assertTrue(printed().contains("There is nothing to choose from."));
    }

    @Test
    void commentLinesAreSkipped() {
        assertEquals(WaiterTask.Kind.WAIT, cli("# turn 1 note\n0\n").chooseWaiterTask(restaurant).kind());
    }

    @Test
    void chefChoosesFromActiveOrdersOrWaits() {
        restaurant.seat(restaurant.customer("C1"), restaurant.table(1));
        restaurant.placeOrder(restaurant.customer("C1"), 1);
        Optional<Order> order = cli("1\n").chooseOrderToCook(restaurant);
        assertEquals("C1's Salad", order.orElseThrow().describe());
        assertEquals(Optional.empty(), cli("0\n").chooseOrderToCook(restaurant));
    }

    @Test
    void endOfInputIsSignalled() {
        assertThrows(InputEndedException.class, () -> cli("").chooseWaiterTask(restaurant));
    }

    @Test
    void echoModePrintsEachAnswerAfterItsPrompt() {
        cli("0\n", true).chooseWaiterTask(restaurant);
        assertTrue(printed().contains("Waiter> 0"));
    }
}
```

`test/restaurantrush/boundary/MainTest.java`:

```java
package restaurantrush.boundary;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import restaurantrush.control.GameSetup;

class MainTest {

    @Test
    void inputEndingEarlyStopsTheGameWithAMessage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Main.play(new ByteArrayInputStream(new byte[0]), new PrintStream(out, true, StandardCharsets.UTF_8), false);
        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Restaurant Rush - " + GameSetup.EDITION), printed);
        assertTrue(printed.contains("Input ended during turn 1; the game was stopped."), printed);
    }
}
```

- [ ] **Step 2: Run them and watch them fail**

Run: `./test.sh`
Expected: FAIL — `cannot find symbol: class GameSetup` / `StatusView` / `GameCLI` / `Main`.

- [ ] **Step 3: Implement `GameSetup`** — `src/restaurantrush/control/GameSetup.java`

```java
package restaurantrush.control;

import java.util.List;
import restaurantrush.entity.Cashier;
import restaurantrush.entity.Chef;
import restaurantrush.entity.Dish;
import restaurantrush.entity.Menu;
import restaurantrush.entity.Money;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.TurnLog;
import restaurantrush.entity.Waiter;

/** Builds the base-setting game. Used by Main and by the scripted-run tests. */
public final class GameSetup {
    public static final String EDITION = "Stage 1: basic restaurant operation";

    private GameSetup() {
    }

    /** The menu in rotation order. */
    public static Menu baseMenu() {
        Dish salad = new Dish("Salad", Money.ofDollars(9), 1);
        Dish burger = new Dish("Burger", Money.ofDollars(15), 1);
        Dish pasta = new Dish("Pasta", Money.ofDollars(18), 2);
        return new Menu(List.of(salad, burger, pasta));
    }

    public static GameController baseGame(GameUI ui) {
        GameConfig config = GameConfig.base();
        Restaurant restaurant = new Restaurant(baseMenu(), config.tableCount(), new TurnLog());
        return new GameController(config, restaurant, ArrivalSchedule.base(),
                new Waiter(), new Chef(), new Cashier(), ui);
    }
}
```

- [ ] **Step 4: Implement `StatusView`** — `src/restaurantrush/boundary/StatusView.java`

```java
package restaurantrush.boundary;

import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import restaurantrush.control.GameConfig;
import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.Order;
import restaurantrush.entity.OrderStatus;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Scoreboard;
import restaurantrush.entity.Table;

/** Turns game state into text for the console. Contains no game rules. */
public class StatusView {

    public String header(int turn, int maxTurns) {
        return "\n========== Turn " + turn + " of " + maxTurns + " ==========";
    }

    public String state(Restaurant restaurant, Scoreboard scoreboard, List<String> availableTasks) {
        StringBuilder text = new StringBuilder();
        text.append("Revenue: ").append(scoreboard.revenue())
                .append(" | Avg satisfaction (paid): ").append(average(scoreboard))
                .append(" | Paid customers: ").append(scoreboard.paidCount()).append('\n');
        text.append("Waiting queue: ").append(queue(restaurant.waitingQueue())).append('\n');
        text.append("Tables: ").append(restaurant.tables().stream()
                .map(this::describeTable).collect(Collectors.joining(" | "))).append('\n');
        text.append("Customers:\n");
        List<Customer> present = restaurant.presentCustomers();
        if (present.isEmpty()) {
            text.append("  (none)\n");
        }
        for (Customer customer : present) {
            text.append("  ").append(describeCustomer(customer)).append('\n');
        }
        text.append("Staff tasks available:");
        for (String task : availableTasks) {
            text.append("\n  - ").append(task);
        }
        return text.toString();
    }

    public String describeCustomer(Customer customer) {
        String where = customer.table() == null ? "" : " at table " + customer.table().number();
        return customer + " - " + statusLabel(customer.status()) + where
                + ", satisfaction " + customer.satisfaction() + ", " + orderLabel(customer.order());
    }

    public String describeOrder(Order order) {
        return order.describe() + " (" + order.price() + "): " + orderStatusLabel(order);
    }

    public String describeTable(Table table) {
        return "Table " + table.number() + (table.isFree() ? " - free" : " - occupied by " + table.occupant().id());
    }

    public String turnEnd(int turn, int maxTurns, Scoreboard scoreboard) {
        return "End of turn " + turn + ": served " + scoreboard.servedCount()
                + " | unhappy departures " + scoreboard.unhappyDepartures()
                + " | revenue " + scoreboard.revenue()
                + " | avg satisfaction " + average(scoreboard)
                + " | turns used " + turn + "/" + maxTurns;
    }

    public String result(boolean victory, Scoreboard scoreboard, GameConfig config) {
        return String.join("\n",
                "",
                "========== GAME OVER: " + (victory ? "VICTORY" : "DEFEAT") + " ==========",
                "Paid customers: " + scoreboard.paidCount() + " (target " + config.targetPaidCustomers() + ") "
                        + mark(config.paidTargetMet(scoreboard.paidCount())),
                "Revenue: " + scoreboard.revenue() + " (target " + config.targetRevenue() + ") "
                        + mark(config.revenueTargetMet(scoreboard.revenue())),
                "Average satisfaction: " + average(scoreboard)
                        + " (target " + config.targetAverageSatisfaction() + ") "
                        + mark(config.satisfactionTargetMet(scoreboard.paidSatisfactionSum(), scoreboard.paidCount())),
                "Served: " + scoreboard.servedCount() + " | Unhappy departures: " + scoreboard.unhappyDepartures()
                        + " | Turns used: " + config.maxTurns() + "/" + config.maxTurns());
    }

    /** Average satisfaction of paid customers to one decimal place, or N/A if nobody has paid. */
    public static String average(Scoreboard scoreboard) {
        OptionalDouble average = scoreboard.averageSatisfaction();
        return average.isPresent() ? String.format(Locale.ROOT, "%.1f", average.getAsDouble()) : "N/A";
    }

    private static String queue(List<Customer> waiting) {
        if (waiting.isEmpty()) {
            return "(empty)";
        }
        return waiting.stream()
                .map(c -> c.id() + " (" + c.typeName() + ", " + c.satisfaction() + ")")
                .collect(Collectors.joining(", "));
    }

    private static String statusLabel(CustomerStatus status) {
        return status == CustomerStatus.SERVED ? "SERVED (eating)" : status.name();
    }

    private static String orderLabel(Order order) {
        if (order == null) {
            return "no order yet";
        }
        return order.item().name() + " " + order.price() + ": " + orderStatusLabel(order);
    }

    private static String orderStatusLabel(Order order) {
        return order.status() == OrderStatus.PLACED ? "cooking " + order.progress() : order.status().name();
    }

    private static String mark(boolean met) {
        return met ? "[met]" : "[missed]";
    }
}
```

- [ ] **Step 5: Implement `InputEndedException` and `GameCLI`**

`src/restaurantrush/boundary/InputEndedException.java`:

```java
package restaurantrush.boundary;

/** Thrown when standard input ends before the game does, e.g. a scripted run that is too short. */
public class InputEndedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InputEndedException() {
        super("Input ended before the game finished");
    }
}
```

`src/restaurantrush/boundary/GameCLI.java`:

```java
package restaurantrush.boundary;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;
import restaurantrush.control.GameConfig;
import restaurantrush.control.GameUI;
import restaurantrush.control.WaiterTask;
import restaurantrush.entity.Customer;
import restaurantrush.entity.Order;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Scoreboard;
import restaurantrush.entity.Table;

/**
 * Console implementation of {@link GameUI}. Shows the state, reads numbered
 * choices and re-prompts on malformed input. Whether a choice is allowed by
 * the game rules is decided by the controller and the domain objects, not here.
 */
public class GameCLI implements GameUI {
    private final Scanner in;
    private final PrintStream out;
    private final boolean echoInput;
    private final StatusView view = new StatusView();

    /**
     * @param echoInput print each answer after its prompt; used for scripted
     *                  runs, where redirected input does not appear on screen
     */
    public GameCLI(InputStream in, PrintStream out, boolean echoInput) {
        this.in = new Scanner(in);
        this.out = out;
        this.echoInput = echoInput;
    }

    @Override
    public void showTurnHeader(int turn, int maxTurns) {
        out.println(view.header(turn, maxTurns));
    }

    @Override
    public void showMessage(String message) {
        out.println(message);
    }

    @Override
    public void showState(Restaurant restaurant, Scoreboard scoreboard, List<String> availableTasks) {
        out.println(view.state(restaurant, scoreboard, availableTasks));
    }

    @Override
    public WaiterTask chooseWaiterTask(Restaurant restaurant) {
        while (true) {
            out.println("Waiter, choose a task:");
            out.println("  1) Seat a waiting customer");
            out.println("  2) Take an order");
            out.println("  3) Serve a READY dish");
            out.println("  0) Wait");
            int choice = readChoice("Waiter> ", 0, 3);
            if (choice == 0) {
                return WaiterTask.waitTurn();
            }
            if (choice == 1) {
                Optional<Customer> customer = pick("Seat which customer? (0 = back)",
                        restaurant.presentCustomers(), view::describeCustomer);
                if (customer.isEmpty()) {
                    continue;
                }
                Optional<Table> table = pick("At which table? (0 = back)", restaurant.tables(), view::describeTable);
                if (table.isPresent()) {
                    return WaiterTask.seat(customer.get(), table.get());
                }
            } else if (choice == 2) {
                Optional<Customer> customer = pick("Take whose order? (0 = back)",
                        restaurant.presentCustomers(), view::describeCustomer);
                if (customer.isPresent()) {
                    return WaiterTask.takeOrder(customer.get());
                }
            } else {
                Optional<Order> order = pick("Serve which dish? (0 = back)",
                        restaurant.activeOrders(), view::describeOrder);
                if (order.isPresent()) {
                    return WaiterTask.serve(order.get());
                }
            }
        }
    }

    @Override
    public Optional<Order> chooseOrderToCook(Restaurant restaurant) {
        return pick("Chef, choose an order to cook (0 = Wait):", restaurant.activeOrders(), view::describeOrder);
    }

    @Override
    public void showTurnEnd(int turn, int maxTurns, Scoreboard scoreboard) {
        out.println(view.turnEnd(turn, maxTurns, scoreboard));
    }

    @Override
    public void showResult(boolean victory, Scoreboard scoreboard, GameConfig config) {
        out.println(view.result(victory, scoreboard, config));
    }

    private <T> Optional<T> pick(String question, List<T> options, Function<T, String> label) {
        if (options.isEmpty()) {
            out.println("There is nothing to choose from.");
            return Optional.empty();
        }
        out.println(question);
        for (int i = 0; i < options.size(); i++) {
            out.println("  " + (i + 1) + ") " + label.apply(options.get(i)));
        }
        int choice = readChoice("> ", 0, options.size());
        return choice == 0 ? Optional.empty() : Optional.of(options.get(choice - 1));
    }

    private int readChoice(String prompt, int min, int max) {
        while (true) {
            out.print(prompt);
            out.flush();
            String line = nextAnswer();
            if (echoInput) {
                out.println(line);
            }
            try {
                int value = Integer.parseInt(line.trim());
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // reported below, together with out-of-range numbers
            }
            out.println("Invalid input '" + line.trim() + "': enter a number from " + min + " to " + max + ".");
        }
    }

    /** The next answer line, skipping '#' lines that annotate scripted runs. */
    private String nextAnswer() {
        while (in.hasNextLine()) {
            String line = in.nextLine();
            if (!line.trim().startsWith("#")) {
                return line;
            }
        }
        throw new InputEndedException();
    }
}
```

- [ ] **Step 6: Implement `Main`** — `src/restaurantrush/boundary/Main.java`

```java
package restaurantrush.boundary;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import restaurantrush.control.GameController;
import restaurantrush.control.GameSetup;

/** Starts a base-setting game on the console. Pass --echo when replaying a scripted run. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        boolean echo = Arrays.asList(args).contains("--echo");
        play(System.in, System.out, echo);
    }

    static void play(InputStream in, PrintStream out, boolean echo) {
        GameController game = GameSetup.baseGame(new GameCLI(in, out, echo));
        out.println("Restaurant Rush - " + GameSetup.EDITION);
        out.println("You are the manager. Type the number of your choice at each prompt.");
        try {
            game.run();
        } catch (InputEndedException e) {
            out.println();
            out.println("Input ended during turn " + game.currentTurn() + "; the game was stopped.");
        }
    }
}
```

- [ ] **Step 7: Run the tests**

Run: `./test.sh`
Expected: PASS — 0 failures.

- [ ] **Step 8: Play one turn by hand to check the console reads well**

Run: `printf '1\n1\n1\n' | ./run.sh --echo`
Expected: the banner, `========== Turn 1 of 18 ==========`, the arrival line for C1, the state block, the Waiter menu with `Waiter> 1`, the customer and table lists, `C1 is seated at table 1.`, `Chef has nothing to cook and waits.`, the turn-1 summary, then turn 2 and `Input ended during turn 2; the game was stopped.`

- [ ] **Step 9: Commit**

```bash
git add src test
git commit -m "$(cat <<'EOF'
Add console UI, game setup and entry point

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

### Task 9: Scripted runs, README and the `stage-1` release

**Files:**
- Create: `runs/victory.txt`, `runs/defeat.txt`, `README.md`
- Test: `test/restaurantrush/boundary/RunScriptsTest.java`

**Interfaces:**
- Consumes: `Main.play(InputStream, PrintStream, boolean)` (Task 8); the prompt layout from Task 8.
- Produces: the two run files replayed by every later stage's `RunScriptsTest`; the `stage-1` tag.

Hand-traced victory run (Stage 1). `present` = customers not yet paid or left, in arrival order. Payment and eating happen after the staff decisions, so a customer who pays this turn is still listed while the Waiter chooses.

| Turn | Waiter | Chef | End of turn |
|---|---|---|---|
| 1 | seat C1 at T1 | auto-wait | C1 92 |
| 2 | take C1 (Salad) | cook → READY | C1 84 |
| 3 | serve C1 | auto-wait | — |
| 4 | seat C2 at T2 | auto-wait | C1 READY_TO_PAY; C2 92 |
| 5 | take C2 (Burger) | cook → READY | C1 pays $9 (84); C2 84 |
| 6 | serve C2 | auto-wait | — |
| 7 | seat C3 at T1 | auto-wait | C2 READY_TO_PAY; C3 92 |
| 8 | take C3 (Pasta) | cook 1/2 | C2 pays $15 (84); C3 84 |
| 9 | auto-wait | cook → READY | C3 76 |
| 10 | serve C3 (C4 waits) | auto-wait | C4 92 |
| 11 | seat C4 at T2 | auto-wait | C3 READY_TO_PAY; C4 84 |
| 12 | take C4 (Salad) | cook → READY | C3 pays $18 (76); C4 76 |
| 13 | serve C4 (C5 waits) | auto-wait | C5 92 |
| 14 | seat C5 at T1 | auto-wait | C4 READY_TO_PAY; C5 84 |
| 15 | take C5 (Burger) | cook → READY | C4 pays $9 (76); C5 76 |
| 16 | serve C5 (C6 waits) | auto-wait | C6 92 |
| 17 | seat C6 at T2 | auto-wait | C5 READY_TO_PAY; C6 84 |
| 18 | take C6 (Pasta) | cook 1/2 | C5 pays $15 (76); C6 76 |

Result: 5 paid, $66.00, satisfaction 84 + 84 + 76 + 76 + 76 = 396 → 79.2, VICTORY.

Defeat run: invalid input on turn 1, then the Waiter waits every turn (the Chef is never asked because no order is placed). C1 leaves at the end of turn 13 and C2 at the end of turn 16; C3 ends at 4. Result: 0 paid, $0.00, N/A, 2 departures, DEFEAT.

- [ ] **Step 1: Write the failing test** — `test/restaurantrush/boundary/RunScriptsTest.java`

```java
package restaurantrush.boundary;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Replays runs/*.txt through the real game and checks the documented results. */
class RunScriptsTest {

    private static String play(String script) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = Files.newInputStream(Path.of("runs", script))) {
            Main.play(in, new PrintStream(out, true, StandardCharsets.UTF_8), true);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static void assertContains(String transcript, String expected) {
        assertTrue(transcript.contains(expected),
                () -> "Expected \"" + expected + "\" in transcript ending:\n"
                        + transcript.substring(Math.max(0, transcript.length() - 3000)));
    }

    @Test
    void victoryRunReproduces() throws IOException {
        String out = play("victory.txt");
        assertContains(out, "GAME OVER: VICTORY");
        assertContains(out, "Paid customers: 5 (target 4) [met]");
        assertContains(out, "Revenue: $66.00 (target $45.00) [met]");
        assertContains(out, "Average satisfaction: 79.2 (target 60) [met]");
        assertContains(out, "Served: 5 | Unhappy departures: 0 | Turns used: 18/18");
        assertFalse(out.contains("Not allowed"), "the victory script should only make legal moves");
        assertFalse(out.contains("Input ended"), "the victory script is too short");
    }

    @Test
    void defeatRunReproduces() throws IOException {
        String out = play("defeat.txt");
        assertContains(out, "Invalid input 'abc'");
        assertContains(out, "There is nothing to choose from.");
        assertContains(out, "GAME OVER: DEFEAT");
        assertContains(out, "Paid customers: 0 (target 4) [missed]");
        assertContains(out, "Revenue: $0.00 (target $45.00) [missed]");
        assertContains(out, "Average satisfaction: N/A (target 60) [missed]");
        assertContains(out, "Served: 0 | Unhappy departures: 2 | Turns used: 18/18");
        assertFalse(out.contains("Input ended"), "the defeat script is too short");
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `./test.sh`
Expected: FAIL — `NoSuchFileException: runs/victory.txt`.

- [ ] **Step 3: Write `runs/victory.txt`**

```
# Restaurant Rush - victory run (base setting)
# Expected: VICTORY - 5 paid, $66.00, average satisfaction 79.2
# Turn 1: seat C1 at table 1
1
1
1
# Turn 2: take C1's order (Salad); Chef cooks it
2
1
1
# Turn 3: serve C1's Salad
3
1
# Turn 4: seat C2 at table 2
1
2
2
# Turn 5: take C2's order (Burger); Chef cooks it
2
2
1
# Turn 6: serve C2's Burger
3
1
# Turn 7: seat C3 at table 1
1
2
1
# Turn 8: take C3's order (Pasta); Chef cooks unit 1
2
2
1
# Turn 9: the Waiter has nothing to do; Chef cooks unit 2
1
# Turn 10: staff constrained - serve C3's Pasta now and let C4 wait
3
1
# Turn 11: seat C4 at table 2
1
2
2
# Turn 12: take C4's order (Salad); Chef cooks it
2
2
1
# Turn 13: serve C4's Salad before seating C5
3
1
# Turn 14: seat C5 at table 1
1
2
1
# Turn 15: take C5's order (Burger); Chef cooks it
2
2
1
# Turn 16: serve C5's Burger before seating C6
3
1
# Turn 17: seat C6 at table 2
1
2
2
# Turn 18: take C6's order (Pasta); Chef starts it - C6 cannot pay in time
2
2
1
```

- [ ] **Step 4: Write `runs/defeat.txt`**

```
# Restaurant Rush - defeat run (base setting)
# Turn 1 shows invalid input being rejected; after that the Waiter waits
# every turn, so nobody is served and two customers leave.
# Expected: DEFEAT - 0 paid, $0.00, average N/A, 2 unhappy departures
# Turn 1: a word, an out-of-range number, Serve with nothing to serve, then Wait
abc
9
3
0
# Turns 2-18: Wait
0
0
0
0
0
0
0
0
0
0
0
0
0
0
0
0
0
```

(17 lines of `0` after the turn 2–18 comment.)

- [ ] **Step 5: Run the tests**

Run: `./test.sh`
Expected: PASS — 0 failures. If a run test fails, replay with `./run.sh --echo < runs/victory.txt` and compare each turn's prompts with the trace table above.

- [ ] **Step 6: Write `README.md`**

````markdown
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
| `victory.txt` | Seat, order and serve each customer as early as possible; serve READY dishes before seating new arrivals | VICTORY: 5 paid, $66.00, average satisfaction 79.2 |
| `defeat.txt` | Shows invalid input being rejected, then leaves the Waiter idle all game | DEFEAT: 0 paid, $0.00, average N/A, 2 unhappy departures |

## How to play

Every prompt is a numbered list: type the number and press Enter.

- **Waiter** (one task per turn): seat a waiting customer at a free table, take a seated customer's order, or serve a READY dish. `0` waits.
- **Chef** (one preparation unit per turn): choose an order to advance. `0` waits.
- **Cashier**: automatic. Takes one payment per turn, oldest first.

If a choice breaks a rule (an occupied table, a dish that is still cooking), the game says why and asks again; the staff member's task is not used up. A staff member with nothing they could do waits automatically.

## Rules as implemented

Base setting: 18 turns, 2 tables, one Waiter, Chef and Cashier, and one customer arriving at the start of turns 1, 4, 7, 10, 13 and 16.

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
````

- [ ] **Step 7: Check that Stage 1 contains no later-stage code**

Run: `grep -rniE "vip|critic|host|combo|happy" src test || echo "clean"`
Expected: `clean`.

- [ ] **Step 8: Commit**

```bash
git add runs test README.md
git commit -m "$(cat <<'EOF'
Add reproducible victory and defeat runs and README

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 9: Ask the user before pushing anything**

Pushing `main`, the branch and the tag publishes the work. Confirm with the user before Step 10.

- [ ] **Step 10: Push, open and merge the PR**

```bash
git push -u origin main
git push -u origin feat/stage1-core
gh pr create --base main --head feat/stage1-core \
  --title "Stage 1: basic restaurant operation" \
  --body "$(cat <<'EOF'
## Summary
- Regular customers, menu with the rotation food-choice rule, Waiter, Chef and Cashier
- The full six-step turn sequence with satisfaction decay, abandonment, payment, victory and defeat
- Console UI, scripted victory and defeat runs, README

## Test plan
- [x] `./test.sh` passes
- [x] `./run.sh --echo < runs/victory.txt` ends in VICTORY ($66.00, 79.2)
- [x] `./run.sh --echo < runs/defeat.txt` ends in DEFEAT

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
gh pr merge feat/stage1-core --merge
git checkout main
git pull --ff-only origin main
```

- [ ] **Step 11: Tag, verify from a clean export, push the tag**

```bash
git tag -a stage-1 -m "Stage 1: basic restaurant operation"
dir=$(mktemp -d) && git archive stage-1 | tar -x -C "$dir" && "$dir/test.sh" && "$dir/run.sh" --echo < "$dir/runs/victory.txt" | tail -6
git push origin stage-1
```

Expected: tests pass in the export and the last lines show `GAME OVER: VICTORY` with `Revenue: $66.00`.

---

# Part B — Stage 2: VIP and Critic customers

### Task 10: VIPCustomer

**Files:**
- Create: `src/restaurantrush/entity/VIPCustomer.java`
- Test: `test/restaurantrush/entity/VIPCustomerTest.java`

**Interfaces:**
- Consumes: `Customer`, `MenuItem`, `Money`, test helper `Dining` (Stage 1).
- Produces: `VIPCustomer(int arrivalNo, int arrivalTurn)` — loses 5 per turn, pays 10% off, `typeName()` = `"VIP"`.

- [ ] **Step 1: Create the stage branch from the released `main`**

```bash
git checkout main
git pull --ff-only origin main
git checkout -b feat/stage2-customers
```

- [ ] **Step 2: Write the failing test** — `test/restaurantrush/entity/VIPCustomerTest.java`

```java
package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VIPCustomerTest {
    private final Dish burger = new Dish("Burger", Money.ofDollars(15), 1);

    @Test
    void losesFivePerWaitingTurn() {
        VIPCustomer vip = new VIPCustomer(2, 4);
        vip.decay();
        assertEquals(95, vip.satisfaction());
        assertEquals("C2 (VIP)", vip.toString());
    }

    @Test
    void paysTenPercentLess() {
        assertEquals(new Money(1350), new VIPCustomer(2, 4).priceFor(burger));
    }

    @Test
    void discountIsLockedIntoTheOrder() {
        Restaurant restaurant = Dining.restaurant(2);
        Order order = Dining.seatedWithOrder(restaurant, new VIPCustomer(2, 4), 4);
        assertEquals("Burger", order.item().name());
        assertEquals(new Money(1350), order.price());
    }
}
```

- [ ] **Step 3: Run it and watch it fail**

Run: `./test.sh`
Expected: FAIL — `cannot find symbol: class VIPCustomer`.

- [ ] **Step 4: Implement** — `src/restaurantrush/entity/VIPCustomer.java`

```java
package restaurantrush.entity;

/** A valued guest: more patient (loses 5 per waiting turn) and pays 10% less. */
public class VIPCustomer extends Customer {
    public static final int LOSS_PER_TURN = 5;
    public static final int DISCOUNT_PERCENT = 10;

    public VIPCustomer(int arrivalNo, int arrivalTurn) {
        super(arrivalNo, arrivalTurn);
    }

    @Override
    public int lossPerTurn() {
        return LOSS_PER_TURN;
    }

    @Override
    public String typeName() {
        return "VIP";
    }

    @Override
    public Money priceFor(MenuItem item) {
        return item.price().percentOff(DISCOUNT_PERCENT);
    }
}
```

- [ ] **Step 5: Run the tests**

Run: `./test.sh`
Expected: PASS — 0 failures.

- [ ] **Step 6: Commit**

```bash
git add src test
git commit -m "$(cat <<'EOF'
Add VIP customer with slower decay and 10% discount

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

### Task 11: CriticCustomer and the service-reaction hook

**Files:**
- Create: `src/restaurantrush/entity/CriticCustomer.java`
- Modify: `src/restaurantrush/entity/Customer.java` (add `onServed` hook), `src/restaurantrush/entity/Restaurant.java` (`serve` calls the hook)
- Test: `test/restaurantrush/entity/CriticCustomerTest.java`; add tests to `test/restaurantrush/entity/RestaurantTest.java` and `test/restaurantrush/control/GameControllerTest.java`

**Interfaces:**
- Consumes: `Customer`, `Order`, `TurnLog`, `Restaurant.serve` (Stage 1); `VIPCustomer` (Task 10).
- Produces: `public void Customer.onServed(Order order, int turn, TurnLog log)` (no-op by default); `CriticCustomer(int arrivalNo, int arrivalTurn)` — loses 12 per turn, full price, `typeName()` = `"Critic"`, −20 satisfaction (floor 0) when `turn - order.readyTurn() > 1`.

- [ ] **Step 1: Write the failing tests**

`test/restaurantrush/entity/CriticCustomerTest.java`:

```java
package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CriticCustomerTest {
    private final Dish pasta = new Dish("Pasta", Money.ofDollars(18), 2);
    private final TurnLog log = new TurnLog();

    private Order readyOnTurn(Customer customer, int turn) {
        Order order = new Order(customer, pasta, pasta.price(), turn - 1);
        order.cook(turn - 1);
        order.cook(turn);
        return order;
    }

    @Test
    void losesTwelvePerWaitingTurnAndPaysFullPrice() {
        CriticCustomer critic = new CriticCustomer(3, 7);
        critic.decay();
        assertEquals(88, critic.satisfaction());
        assertEquals(Money.ofDollars(18), critic.priceFor(pasta));
        assertEquals("C3 (Critic)", critic.toString());
    }

    @Test
    void freshFoodBringsNoComplaint() {
        CriticCustomer critic = new CriticCustomer(3, 7);
        critic.onServed(readyOnTurn(critic, 9), 10, log);
        assertEquals(100, critic.satisfaction());
        assertEquals(List.of("C3 (Critic): served fresh - no complaints."), log.drain());
    }

    @Test
    void coldFoodCostsTwentySatisfaction() {
        CriticCustomer critic = new CriticCustomer(3, 7);
        critic.onServed(readyOnTurn(critic, 9), 11, log);
        assertEquals(80, critic.satisfaction());
        assertEquals(List.of("C3 (Critic): cold food! -20 satisfaction (now 80)."), log.drain());
    }

    @Test
    void penaltyStopsAtZero() {
        CriticCustomer critic = new CriticCustomer(3, 7);
        for (int i = 0; i < 8; i++) {
            critic.decay();
        }
        assertEquals(4, critic.satisfaction());
        critic.onServed(readyOnTurn(critic, 9), 12, log);
        assertEquals(0, critic.satisfaction());
    }

    @Test
    void otherCustomersDoNotReactToService() {
        RegularCustomer regular = new RegularCustomer(1, 1);
        regular.onServed(readyOnTurn(regular, 2), 9, log);
        assertEquals(100, regular.satisfaction());
        assertEquals(List.of(), log.drain());
    }
}
```

Add to `test/restaurantrush/entity/RestaurantTest.java` (inside the class):

```java
    @Test
    void servingTellsTheCustomerSoTheyCanReact() {
        CriticCustomer critic = new CriticCustomer(3, 1);
        Order pasta = Dining.seatedWithOrder(restaurant, critic, 1);
        Dining.cookFully(pasta, 2);
        restaurant.log().drain();
        restaurant.serve(pasta, 4);
        assertEquals(80, critic.satisfaction());
        assertEquals(List.of("C3 (Critic): cold food! -20 satisfaction (now 80)."), restaurant.log().drain());
    }

    @Test
    void servedCriticAtZeroStaysAndIsNotTreatedAsWaiting() {
        CriticCustomer critic = new CriticCustomer(3, 1);
        Order pasta = Dining.seatedWithOrder(restaurant, critic, 1);
        for (int i = 0; i < 8; i++) {
            restaurant.applyWaitingDecay();
        }
        Dining.cookFully(pasta, 9);
        restaurant.serve(pasta, 12);
        assertEquals(0, critic.satisfaction());
        assertTrue(restaurant.applyWaitingDecay().isEmpty());
        assertEquals(CustomerStatus.SERVED, critic.status());
    }
```

Add to `test/restaurantrush/control/GameControllerTest.java` — new imports `restaurantrush.entity.CriticCustomer` and `restaurantrush.entity.VIPCustomer`, then these methods inside the class:

```java
    @Test
    void customerTypesDecayAtTheirOwnRateThroughTheSameController() {
        Restaurant restaurant = restaurantServing(salad);
        GameController game = Games.controller(Games.config(2), restaurant,
                new ArrivalSchedule().add(1, VIPCustomer::new).add(2, CriticCustomer::new), new ScriptedUI());

        game.playTurn(1);
        game.playTurn(2);

        assertEquals(90, restaurant.customer("C1").satisfaction());
        assertEquals(88, restaurant.customer("C2").satisfaction());
    }

    @Test
    void servingTheCriticLateTriggersTheColdFoodPenalty() {
        Restaurant restaurant = restaurantServing(pasta);
        ScriptedUI ui = new ScriptedUI()
                .waiterOn(1, r -> WaiterTask.seat(r.customer("C1"), r.table(1)))
                .waiterOn(2, r -> WaiterTask.takeOrder(r.customer("C1")))
                .chefOn(2, r -> Optional.of(r.customer("C1").order()))
                .chefOn(3, r -> Optional.of(r.customer("C1").order()))
                .waiterOn(5, r -> WaiterTask.serve(r.customer("C1").order()));
        GameController game = Games.controller(Games.config(5), restaurant,
                new ArrivalSchedule().add(1, CriticCustomer::new), ui);

        for (int t = 1; t <= 5; t++) {
            game.playTurn(t);
        }

        // 100 - 4 unserved turns x 12 = 52, then -20 because the Pasta was READY on turn 3
        assertEquals(32, restaurant.customer("C1").satisfaction());
        assertTrue(ui.saw("C1 (Critic): cold food! -20 satisfaction (now 32)."));
    }
```

- [ ] **Step 2: Run them and watch them fail**

Run: `./test.sh`
Expected: FAIL — `cannot find symbol: class CriticCustomer` and `method onServed`.

- [ ] **Step 3: Add the hook to `Customer`**

In `src/restaurantrush/entity/Customer.java`, insert directly after the `chooseItem` method:

```java
    /**
     * Called right after this customer is served. Customers who react to the
     * quality of service override this; by default nothing happens.
     */
    public void onServed(Order order, int turn, TurnLog log) {
        // no reaction by default
    }
```

- [ ] **Step 4: Call the hook from `Restaurant.serve`**

In `src/restaurantrush/entity/Restaurant.java`, replace:

```java
        order.customer().markServed(turn);
        return ActionResult.ok(order.describe() + " is SERVED.");
```

with:

```java
        Customer customer = order.customer();
        customer.markServed(turn);
        customer.onServed(order, turn, log);
        return ActionResult.ok(order.describe() + " is SERVED.");
```

- [ ] **Step 5: Implement** — `src/restaurantrush/entity/CriticCustomer.java`

```java
package restaurantrush.entity;

/**
 * A food critic: impatient (loses 12 per waiting turn), pays full price and
 * judges freshness. A dish can be served the turn after it became READY at
 * the earliest; if it waits any longer it is cold and costs 20 satisfaction.
 */
public class CriticCustomer extends Customer {
    public static final int LOSS_PER_TURN = 12;
    public static final int COLD_FOOD_PENALTY = 20;

    public CriticCustomer(int arrivalNo, int arrivalTurn) {
        super(arrivalNo, arrivalTurn);
    }

    @Override
    public int lossPerTurn() {
        return LOSS_PER_TURN;
    }

    @Override
    public String typeName() {
        return "Critic";
    }

    @Override
    public void onServed(Order order, int turn, TurnLog log) {
        if (turn - order.readyTurn() > 1) {
            reduceSatisfaction(COLD_FOOD_PENALTY);
            log.add(id() + " (Critic): cold food! -" + COLD_FOOD_PENALTY
                    + " satisfaction (now " + satisfaction() + ").");
        } else {
            log.add(id() + " (Critic): served fresh - no complaints.");
        }
    }
}
```

- [ ] **Step 6: Run the tests**

Run: `./test.sh`
Expected: PASS — 0 failures. Stage 1 tests pass unchanged.

- [ ] **Step 7: Confirm the controller has no type checks**

Run: `grep -rnE "instanceof|VIPCustomer|CriticCustomer" src/restaurantrush/control || echo "clean"`
Expected: `clean`. (`ArrivalSchedule` does not mention them until Task 12.)

- [ ] **Step 8: Commit**

```bash
git add src test
git commit -m "$(cat <<'EOF'
Add Critic customer with cold-food penalty via onServed hook

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

### Task 12: Register VIP and Critic arrivals, update runs and README, release `stage-2`

**Files:**
- Modify: `src/restaurantrush/control/ArrivalSchedule.java`, `src/restaurantrush/control/GameSetup.java`
- Modify: `test/restaurantrush/control/ArrivalScheduleTest.java`, `test/restaurantrush/boundary/RunScriptsTest.java`
- Modify: `runs/victory.txt`, `runs/defeat.txt` (comments only), `README.md`

**Interfaces:**
- Consumes: `VIPCustomer`, `CriticCustomer` (Tasks 10–11).
- Produces: base arrivals Regular, VIP, Critic, Regular, Regular, Regular; `GameSetup.EDITION = "Stage 2: VIP and Critic customers"`; the `stage-2` tag.

Stage 2 victory trace with the unchanged inputs: C2 (VIP) is seated on turn 4 (95), orders on 5 (90, bill $13.50) and is served on 6, so 90 is recorded. C3 (Critic) is seated on turn 7 (88), orders on 8 (76), the Pasta is READY on 9 (64) and is served on turn 10, one turn after READY, so it is fresh and 64 is recorded. Result: 5 paid, $9.00 + $13.50 + $18.00 + $9.00 + $15.00 = $64.50, satisfaction 84 + 90 + 64 + 76 + 76 = 390 → 78.0, VICTORY. Defeat run: C1 leaves at the end of turn 13 and the Critic (C3) at the end of turn 15; the VIP ends at 25. Same summary as Stage 1.

- [ ] **Step 1: Update the tests to the Stage 2 expectations**

In `test/restaurantrush/control/ArrivalScheduleTest.java`, replace:

```java
        assertEquals(List.of("Regular", "Regular", "Regular", "Regular", "Regular", "Regular"), types);
```

with:

```java
        assertEquals(List.of("Regular", "VIP", "Critic", "Regular", "Regular", "Regular"), types);
```

In `test/restaurantrush/boundary/RunScriptsTest.java`, replace:

```java
        assertContains(out, "Revenue: $66.00 (target $45.00) [met]");
        assertContains(out, "Average satisfaction: 79.2 (target 60) [met]");
```

with:

```java
        assertContains(out, "Revenue: $64.50 (target $45.00) [met]");
        assertContains(out, "Average satisfaction: 78.0 (target 60) [met]");
        assertContains(out, "C3 (Critic): served fresh - no complaints.");
```

- [ ] **Step 2: Run them and watch them fail**

Run: `./test.sh`
Expected: FAIL — `ArrivalScheduleTest` expected `[Regular, VIP, Critic, ...]`, and `RunScriptsTest.victoryRunReproduces` cannot find `Revenue: $64.50`.

- [ ] **Step 3: Register the new customer types**

In `src/restaurantrush/control/ArrivalSchedule.java`, add the imports:

```java
import restaurantrush.entity.CriticCustomer;
import restaurantrush.entity.VIPCustomer;
```

and replace the whole `base()` method (including its comment) with:

```java
    /**
     * Base setting: one customer at the start of turns 1, 4, 7, 10, 13 and 16.
     * Arrival #2 is a VIP and #3 a Critic; the rest are Regular.
     */
    public static ArrivalSchedule base() {
        return new ArrivalSchedule()
                .add(1, RegularCustomer::new)
                .add(4, VIPCustomer::new)
                .add(7, CriticCustomer::new)
                .add(10, RegularCustomer::new)
                .add(13, RegularCustomer::new)
                .add(16, RegularCustomer::new);
    }
```

In `src/restaurantrush/control/GameSetup.java`, replace:

```java
    public static final String EDITION = "Stage 1: basic restaurant operation";
```

with:

```java
    public static final String EDITION = "Stage 2: VIP and Critic customers";
```

- [ ] **Step 4: Run the tests**

Run: `./test.sh`
Expected: PASS — 0 failures.

- [ ] **Step 5: Update the run-file comments (inputs stay identical)**

In `runs/victory.txt`, replace the first two lines with:

```
# Restaurant Rush - victory run (base setting, C2 is a VIP and C3 a Critic)
# Expected: VICTORY - 5 paid, $64.50, average satisfaction 78.0
```

and replace the line `# Turn 10: staff constrained - serve C3's Pasta now and let C4 wait` with:

```
# Turn 10: staff constrained - serve the Critic's Pasta fresh now (seating C4 first would make it cold: -20)
```

In `runs/defeat.txt`, replace the line `# every turn, so nobody is served and two customers leave.` with:

```
# every turn, so nobody is served; C1 and the Critic (C3) leave.
```

Run: `./test.sh`
Expected: PASS — the inputs did not change, only `#` notes.

- [ ] **Step 6: Update `README.md`**

In the `## Reproducible runs` table, replace the two result cells:
- `VICTORY: 5 paid, $66.00, average satisfaction 79.2` → `VICTORY: 5 paid, $64.50, average satisfaction 78.0`
- `DEFEAT: 0 paid, $0.00, average N/A, 2 unhappy departures` → `DEFEAT: 0 paid, $0.00, average N/A, 2 unhappy departures (C1 and the Critic)`

In `## Rules as implemented`, replace the sentence `Base setting: 18 turns, 2 tables, one Waiter, Chef and Cashier, and one customer arriving at the start of turns 1, 4, 7, 10, 13 and 16.` with:

```markdown
Base setting: 18 turns, 2 tables, one Waiter, Chef and Cashier, and one customer arriving at the start of turns 1, 4, 7, 10, 13 and 16. Arrival #2 (turn 4) is a VIP and arrival #3 (turn 7) is a Critic; the others are Regular.

| Customer | Satisfaction lost per waiting turn | Bill | Special behaviour |
|---|---|---|---|
| Regular | 8 | Full price | — |
| VIP | 5 | 10% off | — |
| Critic | 12 | Full price | Cold-food penalty: if the dish is served more than one turn after it became READY, the Critic loses 20 satisfaction |
```

Append to the numbered interpretations list:

```markdown
7. A served customer never leaves. A Critic whose satisfaction drops to 0 because of cold food still eats and pays; abandonment applies only to customers who have not been served.
8. Discounts are rounded half-up to the cent.
```

In `## Stages`, add the row:

```markdown
| `stage-2` | VIP and Critic customers |
```

Insert a new section directly before `## Video timestamps`:

```markdown
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
```

- [ ] **Step 7: Check that Stage 2 contains no Stage 3 code**

Run: `grep -rniE "host|combo|happy" src test || echo "clean"`
Expected: `clean`.

- [ ] **Step 8: Commit**

```bash
git add src test runs README.md
git commit -m "$(cat <<'EOF'
Register VIP and Critic arrivals and document the stage 2 changes

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 9: Push, open and merge the PR**

```bash
git push -u origin feat/stage2-customers
gh pr create --base main --head feat/stage2-customers \
  --title "Stage 2: VIP and Critic customers" \
  --body "$(cat <<'EOF'
## Summary
- VIPCustomer (loses 5 per turn, 10% off) and CriticCustomer (loses 12 per turn, cold-food penalty)
- Customer.onServed hook, called from Restaurant.serve; no type checks in the controller
- Arrival #2 is a VIP and #3 a Critic; README documents the 1 -> 2 changes

## Test plan
- [x] `./test.sh` passes, including all Stage 1 tests
- [x] `./run.sh --echo < runs/victory.txt` ends in VICTORY ($64.50, 78.0)
- [x] `./run.sh --echo < runs/defeat.txt` ends in DEFEAT

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
gh pr merge feat/stage2-customers --merge
git checkout main
git pull --ff-only origin main
```

- [ ] **Step 10: Tag, verify from a clean export, push the tag**

```bash
git tag -a stage-2 -m "Stage 2: VIP and Critic customers"
dir=$(mktemp -d) && git archive stage-2 | tar -x -C "$dir" && "$dir/test.sh" && "$dir/run.sh" --echo < "$dir/runs/victory.txt" | tail -6
git push origin stage-2
```

Expected: tests pass in the export and the last lines show `GAME OVER: VICTORY` with `Revenue: $64.50`.

---

# Part C — Stage 3: Host, ComboMeal and Happy Hour

### Task 13: ComboMeal

**Files:**
- Create: `src/restaurantrush/entity/ComboMeal.java`
- Test: `test/restaurantrush/entity/ComboMealTest.java`

**Interfaces:**
- Consumes: `MenuItem`, `Dish`, `Menu`, `Money` (Stage 1).
- Produces: `ComboMeal(MenuItem first, MenuItem second) extends MenuItem` with `first()`, `second()`; name `"Salad + Pasta"`; `prepUnits()` = sum; `price()` = sum `percentOff(10)`.

- [ ] **Step 1: Create the stage branch from the released `main`**

```bash
git checkout main
git pull --ff-only origin main
git checkout -b feat/stage3-host-combo-happyhour
```

- [ ] **Step 2: Write the failing test** — `test/restaurantrush/entity/ComboMealTest.java`

```java
package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;

class ComboMealTest {
    private final Dish salad = new Dish("Salad", Money.ofDollars(9), 1);
    private final Dish burger = new Dish("Burger", Money.ofDollars(15), 1);
    private final Dish pasta = new Dish("Pasta", Money.ofDollars(18), 2);

    @Test
    void briefExampleSaladPlusPasta() {
        ComboMeal combo = new ComboMeal(salad, pasta);
        assertEquals("Salad + Pasta", combo.name());
        assertEquals(3, combo.prepUnits());
        assertEquals(new Money(2430), combo.price());
        assertEquals("Salad + Pasta ($24.30, 3 units)", combo.toString());
    }

    @Test
    void everyBaseComboIsNinetyPercentOfItsParts() {
        assertEquals(new Money(2160), new ComboMeal(salad, burger).price());
        assertEquals(new Money(2970), new ComboMeal(burger, pasta).price());
    }

    @Test
    void priceRoundsHalfUpToTheCent() {
        Dish nickel = new Dish("Mint", new Money(5), 1);
        Dish free = new Dish("Water", Money.ZERO, 1);
        assertEquals(new Money(5), new ComboMeal(nickel, free).price()); // 4.5 cents -> 5
    }

    @Test
    void rotationReachesCombosWithoutAnyChangeToTheRule() {
        ComboMeal saladPasta = new ComboMeal(salad, pasta);
        Menu menu = new Menu(List.of(salad, burger, pasta,
                new ComboMeal(salad, burger), saladPasta, new ComboMeal(burger, pasta)));
        assertSame(saladPasta, menu.itemFor(5));
        assertSame(saladPasta, new RegularCustomer(5, 13).chooseItem(menu));
    }
}
```

- [ ] **Step 3: Run it and watch it fail**

Run: `./test.sh`
Expected: FAIL — `cannot find symbol: class ComboMeal`.

- [ ] **Step 4: Implement** — `src/restaurantrush/entity/ComboMeal.java`

```java
package restaurantrush.entity;

/**
 * Two existing menu items sold together. Preparation time is the sum of the
 * parts; the price is 90% of their combined price, rounded to the cent.
 */
public class ComboMeal extends MenuItem {
    public static final int DISCOUNT_PERCENT = 10;

    private final MenuItem first;
    private final MenuItem second;

    public ComboMeal(MenuItem first, MenuItem second) {
        super(first.name() + " + " + second.name());
        this.first = first;
        this.second = second;
    }

    public MenuItem first() {
        return first;
    }

    public MenuItem second() {
        return second;
    }

    @Override
    public Money price() {
        return first.price().plus(second.price()).percentOff(DISCOUNT_PERCENT);
    }

    @Override
    public int prepUnits() {
        return first.prepUnits() + second.prepUnits();
    }
}
```

- [ ] **Step 5: Run the tests**

Run: `./test.sh`
Expected: PASS — 0 failures.

- [ ] **Step 6: Commit**

```bash
git add src test
git commit -m "$(cat <<'EOF'
Add ComboMeal composed of two menu items

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

### Task 14: HappyHour pricing and recovery

**Files:**
- Create: `src/restaurantrush/entity/HappyHour.java`
- Modify: `src/restaurantrush/entity/Customer.java` (add `recover`), `src/restaurantrush/entity/Restaurant.java` (Happy Hour field, constructor, pricing, recovery)
- Test: `test/restaurantrush/entity/HappyHourTest.java`; add tests to `test/restaurantrush/entity/RestaurantTest.java`

**Interfaces:**
- Consumes: `Money`, `ActionResult`, `Customer`, `Restaurant`, `VIPCustomer`, `Dining`.
- Produces:
  - `HappyHour()`: constants `DISCOUNT_PERCENT = 20`, `RECOVERY = 15`, `DURATION_TURNS = 2`; `boolean isAvailable()`, `boolean isActive()`, `int turnsRemaining()`, `ActionResult activate()`, `void endTurn()`, `Money adjust(Money)`, `String status()`
  - package-private `void Customer.recover(int points)` (caps at 100)
  - `Restaurant(Menu, int tableCount, TurnLog log, HappyHour happyHour)`; the existing three-argument constructor stays and uses a fresh, never-activated `HappyHour`; `HappyHour happyHour()`; `void recoverAwaitingCustomers(int amount)`

- [ ] **Step 1: Write the failing tests**

`test/restaurantrush/entity/HappyHourTest.java`:

```java
package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HappyHourTest {
    private final HappyHour happyHour = new HappyHour();

    @Test
    void startsAvailableAndChangesNoPrices() {
        assertTrue(happyHour.isAvailable());
        assertFalse(happyHour.isActive());
        assertEquals(Money.ofDollars(15), happyHour.adjust(Money.ofDollars(15)));
        assertEquals("available (once per game)", happyHour.status());
    }

    @Test
    void lastsTheActivationTurnAndTheNext() {
        assertTrue(happyHour.activate().success());
        assertEquals("ACTIVE - 2 turns left including this one", happyHour.status());
        happyHour.endTurn();
        assertTrue(happyHour.isActive());
        assertEquals("ACTIVE - 1 turn left including this one", happyHour.status());
        happyHour.endTurn();
        assertFalse(happyHour.isActive());
        assertEquals("used", happyHour.status());
    }

    @Test
    void canOnlyBeActivatedOncePerGame() {
        happyHour.activate();
        happyHour.endTurn();
        happyHour.endTurn();
        assertFalse(happyHour.isAvailable());
        assertEquals("Happy Hour has already been used this game", happyHour.activate().message());
    }

    @Test
    void takesTwentyPercentOffAfterAnyCustomerDiscount() {
        happyHour.activate();
        assertEquals(new Money(1080), happyHour.adjust(Money.ofDollars(15).percentOff(10)));
    }
}
```

Add to `test/restaurantrush/entity/RestaurantTest.java` (inside the class):

```java
    @Test
    void happyHourPriceAppliesAfterTheVipDiscountAndIsLocked() {
        HappyHour happyHour = new HappyHour();
        Restaurant promoted = new Restaurant(restaurant.menu(), 2, new TurnLog(), happyHour);
        Customer vip = new VIPCustomer(2, 4);
        promoted.admit(vip);
        promoted.seat(vip, promoted.table(1));
        happyHour.activate();

        ActionResult result = promoted.placeOrder(vip, 4);

        assertEquals("C2 orders Burger ($15.00, 1 unit) - bill $10.80 (Happy Hour price).", result.message());
        happyHour.endTurn();
        happyHour.endTurn();
        assertEquals(new Money(1080), vip.order().price());
    }

    @Test
    void ordersOutsideHappyHourPayTheNormalPrice() {
        Order order = Dining.seatedWithOrder(restaurant, c2, 4);
        assertEquals(Money.ofDollars(15), order.price());
    }

    @Test
    void happyHourRecoveryHelpsOnlyCustomersStillWaitingAndCapsAt100() {
        Customer c3 = new RegularCustomer(3, 1);
        restaurant.admit(c1);
        restaurant.admit(c2);
        restaurant.applyWaitingDecay();
        restaurant.applyWaitingDecay();
        restaurant.seat(c1, restaurant.table(1));
        restaurant.placeOrder(c1, 3);
        Dining.cookFully(c1.order(), 3);
        restaurant.serve(c1.order(), 4);
        restaurant.admit(c3);

        restaurant.recoverAwaitingCustomers(15);

        assertEquals(84, c1.satisfaction());
        assertEquals(99, c2.satisfaction());
        assertEquals(100, c3.satisfaction());
    }
```

- [ ] **Step 2: Run them and watch them fail**

Run: `./test.sh`
Expected: FAIL — `cannot find symbol: class HappyHour` / `method recoverAwaitingCustomers`.

- [ ] **Step 3: Implement** — `src/restaurantrush/entity/HappyHour.java`

```java
package restaurantrush.entity;

/**
 * A once-per-game promotion the manager may switch on at the start of a turn.
 * It lasts that turn and the next. Orders taken while it is active get 20%
 * off (after any customer discount), and customers still waiting for a table
 * or food recover 15 satisfaction at the start of each active turn.
 */
public class HappyHour {
    public static final int DISCOUNT_PERCENT = 20;
    public static final int RECOVERY = 15;
    public static final int DURATION_TURNS = 2;

    private boolean used;
    private int turnsRemaining;

    public boolean isAvailable() {
        return !used;
    }

    public boolean isActive() {
        return turnsRemaining > 0;
    }

    /** Active turns left, counting the current one. */
    public int turnsRemaining() {
        return turnsRemaining;
    }

    public ActionResult activate() {
        if (used) {
            return ActionResult.fail("Happy Hour has already been used this game");
        }
        used = true;
        turnsRemaining = DURATION_TURNS;
        return ActionResult.ok("Happy Hour is ON for this turn and the next: new orders get "
                + DISCOUNT_PERCENT + "% off and waiting customers recover " + RECOVERY + " satisfaction each turn.");
    }

    /** Called once at the end of every turn. */
    public void endTurn() {
        if (turnsRemaining > 0) {
            turnsRemaining--;
        }
    }

    /** The price after the Happy Hour discount, or unchanged when it is not active. */
    public Money adjust(Money price) {
        return isActive() ? price.percentOff(DISCOUNT_PERCENT) : price;
    }

    public String status() {
        if (isActive()) {
            return "ACTIVE - " + turnsRemaining + (turnsRemaining == 1 ? " turn" : " turns")
                    + " left including this one";
        }
        return used ? "used" : "available (once per game)";
    }
}
```

- [ ] **Step 4: Add `recover` to `Customer`**

In `src/restaurantrush/entity/Customer.java`, insert directly after the `reduceSatisfaction` method:

```java
    void recover(int points) {
        satisfaction = Math.min(MAX_SATISFACTION, satisfaction + points);
    }
```

- [ ] **Step 5: Give `Restaurant` the Happy Hour**

In `src/restaurantrush/entity/Restaurant.java`:

1. Add the field after `private final TurnLog log;`:

```java
    private final HappyHour happyHour;
```

2. Replace the whole constructor:

```java
    public Restaurant(Menu menu, int tableCount, TurnLog log) {
        if (tableCount < 1) {
            throw new IllegalArgumentException("A restaurant needs at least one table");
        }
        this.menu = menu;
        this.log = log;
        for (int number = 1; number <= tableCount; number++) {
            tables.add(new Table(number));
        }
    }
```

with:

```java
    /** A restaurant whose Happy Hour is never switched on. */
    public Restaurant(Menu menu, int tableCount, TurnLog log) {
        this(menu, tableCount, log, new HappyHour());
    }

    public Restaurant(Menu menu, int tableCount, TurnLog log, HappyHour happyHour) {
        if (tableCount < 1) {
            throw new IllegalArgumentException("A restaurant needs at least one table");
        }
        this.menu = menu;
        this.log = log;
        this.happyHour = happyHour;
        for (int number = 1; number <= tableCount; number++) {
            tables.add(new Table(number));
        }
    }
```

3. Add a getter after `log()`:

```java
    public HappyHour happyHour() {
        return happyHour;
    }
```

4. In `placeOrder`, replace:

```java
        Money price = customer.priceFor(item);
```

with:

```java
        Money price = happyHour.adjust(customer.priceFor(item));
```

and replace:

```java
        return ActionResult.ok(customer.id() + " orders " + item + " - bill " + price + ".");
```

with:

```java
        String promotion = happyHour.isActive() ? " (Happy Hour price)" : "";
        return ActionResult.ok(customer.id() + " orders " + item + " - bill " + price + promotion + ".");
```

5. Add this method after `finishEating`:

```java
    /** Happy Hour: everyone still waiting for a table or food recovers satisfaction (capped at 100). */
    public void recoverAwaitingCustomers(int amount) {
        for (Customer customer : customers) {
            if (customer.isAwaitingService()) {
                customer.recover(amount);
                log.add(customer.id() + " enjoys Happy Hour: +" + amount
                        + " satisfaction (now " + customer.satisfaction() + ").");
            }
        }
    }
```

- [ ] **Step 6: Run the tests**

Run: `./test.sh`
Expected: PASS — 0 failures. Stage 1 and 2 tests still use the three-argument constructor and pass unchanged.

- [ ] **Step 7: Commit**

```bash
git add src test
git commit -m "$(cat <<'EOF'
Add Happy Hour pricing and recovery to the restaurant

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

### Task 15: Host, Happy Hour decisions and the Stage 3 game wiring

The controller, the UI interface, the console and the game setup change together here so every commit compiles and every test (including the scripted runs) stays green.

**Files:**
- Create: `src/restaurantrush/entity/Host.java`, `src/restaurantrush/control/Seating.java`
- Modify (full replacement): `src/restaurantrush/control/GameUI.java`, `src/restaurantrush/control/GameController.java`, `src/restaurantrush/control/GameSetup.java`, `test/restaurantrush/control/ScriptedUI.java`, `test/restaurantrush/control/Games.java`, `runs/victory.txt`, `runs/defeat.txt`
- Modify (edits): `src/restaurantrush/boundary/GameCLI.java`, `src/restaurantrush/boundary/StatusView.java`, `test/restaurantrush/boundary/RunScriptsTest.java`
- Test: `test/restaurantrush/entity/HostTest.java`; add tests to `GameControllerTest`, `GameCLITest`, `StatusViewTest`

**Interfaces:**
- Consumes: `HappyHour`, `Restaurant.happyHour()`, `Restaurant.recoverAwaitingCustomers(int)` (Task 14); `ComboMeal` (Task 13).
- Produces:
  - `Host() extends Staff`: `ActionResult seat(Restaurant, Customer, Table)`
  - `record Seating(Customer customer, Table table)` (control)
  - `GameUI` gains `boolean askActivateHappyHour(HappyHour)` and `Optional<Seating> chooseHostSeating(Restaurant)`
  - `GameController(GameConfig, Restaurant, ArrivalSchedule, Host, Waiter, Chef, Cashier, GameUI)`
  - `ScriptedUI` gains `hostOn(int turn, Function<Restaurant, Optional<Seating>>)`, `happyHourOn(int turn)`, fields `hostPrompts`, `happyHourPrompts`
  - `GameSetup.EDITION = "Stage 3: Host, combo meals and Happy Hour"`; base menu has three combos

**Prompt layout additions:** at the start of every turn while Happy Hour is unused: `1) Activate now` / `0) Not yet`. Then, when someone is waiting and a table is free, the Host prompt: pick from `presentCustomers()` (`0` = Wait), then from `tables()` (`0` = back to the Host customer list). Then the Waiter and the Chef as before.

Hand-traced Stage 3 victory run (HH = Happy Hour prompt answer; host seats at arrival; `present` as before):

| Turn | HH | Host | Waiter | Chef | Notes |
|---|---|---|---|---|---|
| 1 | 0 | C1 → T1 | take C1 (Salad) | cook → READY | C1 92 |
| 2 | 0 | auto | serve C1 | auto | |
| 3 | 0 | auto | auto | auto | C1 READY_TO_PAY |
| 4 | 0 | C2 → T2 | take C2 (Burger, $13.50) | cook → READY | C1 pays $9 (92); C2 95 |
| 5 | 0 | auto | serve C2 | auto | |
| 6 | 0 | auto | auto | auto | C2 READY_TO_PAY |
| 7 | 0 | C3 → T1 | take C3 (Pasta) | cook 1/2 | C2 pays $13.50 (95); C3 88 |
| 8 | 1 | auto | auto | cook → READY | C3 88 → 100 → 88 |
| 9 | — | auto | serve C3 (fresh) | auto | C3 88 → 100, served |
| 10 | — | C4 → T2 | take C4 (Salad + Burger, $21.60) | cook 1/2 | C3 READY_TO_PAY; C4 92 |
| 11 | — | auto | auto | cook → READY | C3 pays $18 (100); C4 84 |
| 12 | — | auto | serve C4 | auto | |
| 13 | — | C5 → T1 | take C5 (Salad + Pasta, $24.30) | cook 1/3 | C4 READY_TO_PAY; C5 92 |
| 14 | — | auto | auto | cook 2/3 | C4 pays $21.60 (84); C5 84 |
| 15 | — | auto | auto | cook → READY | C5 76 |
| 16 | — | C6 → T2 | serve C5 | auto | C6 92 |
| 17 | — | auto | take C6 (Burger + Pasta) | cook 1/3 | C5 READY_TO_PAY; C6 84 |
| 18 | — | auto | auto | cook 2/3 | C5 pays $24.30 (76); C6 76 |

Result: 5 paid, $86.40, satisfaction 92 + 95 + 100 + 84 + 76 = 447 → 89.4, VICTORY. Without the Host, C5 would be seated on 13, order on 14, be READY on 16, served on 17 and could not pay by turn 18.

Stage 3 defeat run: each turn answers `0` to Happy Hour, `0` to the Host and `0` to the Waiter (turn 1 adds the invalid inputs). C1 leaves at the end of turn 13 and the Critic at the end of turn 15: same summary as before.

- [ ] **Step 1: Write the failing tests**

`test/restaurantrush/entity/HostTest.java`:

```java
package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HostTest {
    private final Restaurant restaurant = Dining.restaurant(2);
    private final Customer c1 = new RegularCustomer(1, 1);
    private final Customer c2 = new RegularCustomer(2, 1);

    @Test
    void seatsAtMostOneCustomerPerTurn() {
        Host host = new Host();
        restaurant.admit(c1);
        restaurant.admit(c2);
        assertTrue(host.seat(restaurant, c1, restaurant.table(1)).success());
        assertEquals("Host has already completed a task this turn",
                host.seat(restaurant, c2, restaurant.table(2)).message());
    }

    @Test
    void followsTheSameSeatingRulesAsTheWaiter() {
        Host host = new Host();
        restaurant.admit(c1);
        restaurant.admit(c2);
        new Waiter().seat(restaurant, c1, restaurant.table(1));
        ActionResult result = host.seat(restaurant, c2, restaurant.table(1));
        assertFalse(result.success());
        assertEquals("Table 1 is occupied by C1", result.message());
        assertFalse(host.hasActed());
    }
}
```

Add to `test/restaurantrush/control/GameControllerTest.java` (inside the class; no new imports, `Seating` is in the same package):

```java
    @Test
    void hostSeatsOnArrivalSoTheWaiterCanTakeTheOrderTheSameTurn() {
        Restaurant restaurant = restaurantServing(salad);
        ScriptedUI ui = new ScriptedUI()
                .hostOn(1, r -> Optional.of(new Seating(r.customer("C1"), r.table(1))))
                .waiterOn(1, r -> WaiterTask.takeOrder(r.customer("C1")))
                .chefOn(1, r -> Optional.of(r.customer("C1").order()));
        GameController game = Games.controller(Games.config(1), restaurant, oneRegularOnTurnOne(), ui);

        game.playTurn(1);

        assertEquals(OrderStatus.READY, restaurant.customer("C1").order().status());
    }

    @Test
    void hostAndWaiterCanEachSeatSomeoneInTheSameTurn() {
        Restaurant restaurant = restaurantServing(salad);
        ScriptedUI ui = new ScriptedUI()
                .hostOn(2, r -> Optional.of(new Seating(r.customer("C1"), r.table(1))))
                .waiterOn(2, r -> WaiterTask.seat(r.customer("C2"), r.table(2)));
        GameController game = Games.controller(Games.config(2), restaurant,
                new ArrivalSchedule().add(1, RegularCustomer::new).add(2, RegularCustomer::new), ui);

        game.playTurn(1);
        game.playTurn(2);

        assertEquals(CustomerStatus.SEATED, restaurant.customer("C1").status());
        assertEquals(CustomerStatus.SEATED, restaurant.customer("C2").status());
    }

    @Test
    void happyHourIsOfferedUntilUsedAndLastsTwoTurns() {
        Restaurant restaurant = restaurantServing(salad);
        ScriptedUI ui = new ScriptedUI().happyHourOn(3);
        GameController game = Games.controller(Games.config(5), restaurant, oneRegularOnTurnOne(), ui);
        Customer c1 = null;

        for (int t = 1; t <= 5; t++) {
            game.playTurn(t);
            c1 = restaurant.customer("C1");
            if (t == 2) {
                assertEquals(84, c1.satisfaction());
            }
            if (t == 3) {
                assertEquals(91, c1.satisfaction()); // 84 + 15, then -8
            }
            if (t == 4) {
                assertEquals(92, c1.satisfaction()); // 91 + 15 capped at 100, then -8
            }
        }

        assertEquals(84, c1.satisfaction()); // turn 5: no recovery, -8
        assertEquals(3, ui.happyHourPrompts);
        assertTrue(ui.saw("Happy Hour is ON for this turn and the next"));
    }

    @Test
    void happyHourOnTheLastTurnStillEndsTheGameNormally() {
        ScriptedUI ui = new ScriptedUI().happyHourOn(2);
        GameController game = Games.controller(Games.config(2), restaurantServing(salad), oneRegularOnTurnOne(), ui);

        assertFalse(game.run());
        assertEquals(Boolean.FALSE, ui.victory);
        assertEquals(2, ui.happyHourPrompts);
    }
```

Add to `test/restaurantrush/boundary/GameCLITest.java` — new imports `restaurantrush.control.Seating`, `restaurantrush.entity.HappyHour` and `static org.junit.jupiter.api.Assertions.assertFalse`, then inside the class:

```java
    @Test
    void hostSeatingNamesTheCustomerAndTable() {
        Seating seating = cli("2\n2\n").chooseHostSeating(restaurant).orElseThrow();
        assertEquals("C2", seating.customer().id());
        assertEquals(2, seating.table().number());
    }

    @Test
    void hostCanWaitOrGoBackFromTheTableList() {
        assertEquals(Optional.empty(), cli("0\n").chooseHostSeating(restaurant));
        assertEquals(Optional.empty(), cli("1\n0\n0\n").chooseHostSeating(restaurant));
    }

    @Test
    void happyHourQuestionIsYesOrNo() {
        assertTrue(cli("1\n").askActivateHappyHour(new HappyHour()));
        assertFalse(cli("0\n").askActivateHappyHour(new HappyHour()));
    }
```

Add to `test/restaurantrush/boundary/StatusViewTest.java` (inside the class):

```java
    @Test
    void stateShowsHappyHourStatus() {
        String state = view.state(restaurant, scoreboard, List.of());
        assertTrue(state.contains("Happy Hour: available (once per game)"), state);
    }
```

In `test/restaurantrush/boundary/RunScriptsTest.java`, replace:

```java
        assertContains(out, "Revenue: $64.50 (target $45.00) [met]");
        assertContains(out, "Average satisfaction: 78.0 (target 60) [met]");
        assertContains(out, "C3 (Critic): served fresh - no complaints.");
```

with:

```java
        assertContains(out, "Revenue: $86.40 (target $45.00) [met]");
        assertContains(out, "Average satisfaction: 89.4 (target 60) [met]");
        assertContains(out, "C3 (Critic): served fresh - no complaints.");
        assertContains(out, "C5 orders Salad + Pasta ($24.30, 3 units) - bill $24.30.");
        assertContains(out, "Happy Hour: ACTIVE - 1 turn left including this one");
```

- [ ] **Step 2: Run them and watch them fail**

Run: `./test.sh`
Expected: FAIL — `cannot find symbol: class Host` / `class Seating` / `method hostOn` / `method chooseHostSeating`.

- [ ] **Step 3: Implement `Host` and `Seating`**

`src/restaurantrush/entity/Host.java`:

```java
package restaurantrush.entity;

/** Seats at most one queued customer per turn, in addition to the Waiter's task. */
public class Host extends Staff {

    public Host() {
        super("Host");
    }

    /** Uses the same seating rules as the Waiter, so the two can never disagree. */
    public ActionResult seat(Restaurant restaurant, Customer customer, Table table) {
        return perform(() -> restaurant.seat(customer, table));
    }
}
```

`src/restaurantrush/control/Seating.java`:

```java
package restaurantrush.control;

import restaurantrush.entity.Customer;
import restaurantrush.entity.Table;

/** The manager's instruction to the Host: who to seat, and where. */
public record Seating(Customer customer, Table table) {
}
```

- [ ] **Step 4: Replace `GameUI`** — `src/restaurantrush/control/GameUI.java`

```java
package restaurantrush.control;

import java.util.List;
import java.util.Optional;
import restaurantrush.entity.HappyHour;
import restaurantrush.entity.Order;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Scoreboard;

/**
 * The manager's side of the game, as the controller sees it. The controller
 * decides when each question is asked; implementations only display state
 * and collect answers.
 */
public interface GameUI {

    void showTurnHeader(int turn, int maxTurns);

    void showMessage(String message);

    void showState(Restaurant restaurant, Scoreboard scoreboard, List<String> availableTasks);

    /** Asked at the start of a turn while Happy Hour is still unused. */
    boolean askActivateHappyHour(HappyHour happyHour);

    /** Who the Host should seat and where, or empty to wait. */
    Optional<Seating> chooseHostSeating(Restaurant restaurant);

    WaiterTask chooseWaiterTask(Restaurant restaurant);

    /** The order the Chef should advance by one unit, or empty to wait. */
    Optional<Order> chooseOrderToCook(Restaurant restaurant);

    void showTurnEnd(int turn, int maxTurns, Scoreboard scoreboard);

    void showResult(boolean victory, Scoreboard scoreboard, GameConfig config);
}
```

- [ ] **Step 5: Replace `GameController`** — `src/restaurantrush/control/GameController.java`

```java
package restaurantrush.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import restaurantrush.entity.ActionResult;
import restaurantrush.entity.Cashier;
import restaurantrush.entity.Chef;
import restaurantrush.entity.Customer;
import restaurantrush.entity.HappyHour;
import restaurantrush.entity.Host;
import restaurantrush.entity.Order;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Scoreboard;
import restaurantrush.entity.Waiter;

/**
 * Runs the game: every turn follows the brief's six steps in order. The
 * controller owns the step order and the "an invalid choice does not use up
 * the task" rule; the rules for each action live in the staff and domain
 * objects.
 */
public class GameController {
    private final GameConfig config;
    private final Restaurant restaurant;
    private final Scoreboard scoreboard;
    private final ArrivalSchedule schedule;
    private final Host host;
    private final Waiter waiter;
    private final Chef chef;
    private final Cashier cashier;
    private final GameUI ui;
    private int turn;

    public GameController(GameConfig config, Restaurant restaurant, ArrivalSchedule schedule,
                          Host host, Waiter waiter, Chef chef, Cashier cashier, GameUI ui) {
        this.config = config;
        this.restaurant = restaurant;
        this.scoreboard = new Scoreboard(restaurant);
        this.schedule = schedule;
        this.host = host;
        this.waiter = waiter;
        this.chef = chef;
        this.cashier = cashier;
        this.ui = ui;
    }

    public int currentTurn() {
        return turn;
    }

    public Scoreboard scoreboard() {
        return scoreboard;
    }

    /** Plays every turn (there is no early ending) and returns true on victory. */
    public boolean run() {
        for (int t = 1; t <= config.maxTurns(); t++) {
            playTurn(t);
        }
        boolean victory = config.isVictory(scoreboard);
        ui.showResult(victory, scoreboard, config);
        return victory;
    }

    /** Plays one turn. Package-private so tests can check the state between turns. */
    void playTurn(int t) {
        turn = t;
        host.startTurn();
        waiter.startTurn();
        chef.startTurn();
        cashier.startTurn();

        ui.showTurnHeader(turn, config.maxTurns());
        schedule.arrivalAt(turn).ifPresent(restaurant::admit);      // 1 arrivals
        flushLog();
        ui.showState(restaurant, scoreboard, availableTasks());
        happyHourStep();                                            // promotion decided at turn start

        hostStep();                                                 // 2-3 the Host, then the Waiter before the Chef
        waiterStep();
        chefStep();

        cashier.collectPayment(restaurant, turn);                   // 4 payment, then eating
        restaurant.finishEating(turn);
        restaurant.applyWaitingDecay();                             // 5 waiting and abandonment
        restaurant.happyHour().endTurn();
        flushLog();
        ui.showTurnEnd(turn, config.maxTurns(), scoreboard);        // 6 results
    }

    private void happyHourStep() {
        HappyHour happyHour = restaurant.happyHour();
        if (happyHour.isAvailable() && ui.askActivateHappyHour(happyHour)) {
            report(happyHour.activate());
        }
        if (happyHour.isActive()) {
            restaurant.recoverAwaitingCustomers(HappyHour.RECOVERY);
            flushLog();
        }
    }

    private void hostStep() {
        if (!restaurant.canSeatSomeone()) {
            ui.showMessage("Host has no one to seat and waits.");
            return;
        }
        while (true) {
            Optional<Seating> choice = ui.chooseHostSeating(restaurant);
            ActionResult result = choice.map(s -> host.seat(restaurant, s.customer(), s.table()))
                    .orElse(ActionResult.ok("Host waits."));
            report(result);
            if (result.success()) {
                return;
            }
        }
    }

    private void waiterStep() {
        if (!waiterHasWork()) {
            ui.showMessage("Waiter has nothing to do this turn and waits.");
            return;
        }
        while (true) {
            ActionResult result = execute(ui.chooseWaiterTask(restaurant));
            report(result);
            if (result.success()) {
                return;
            }
        }
    }

    private ActionResult execute(WaiterTask task) {
        switch (task.kind()) {
            case SEAT:
                return waiter.seat(restaurant, task.customer(), task.table());
            case TAKE_ORDER:
                return waiter.takeOrder(restaurant, task.customer(), turn);
            case SERVE:
                return waiter.serve(restaurant, task.order(), turn);
            default:
                return ActionResult.ok("Waiter waits.");
        }
    }

    private void chefStep() {
        if (restaurant.cookableOrders().isEmpty()) {
            ui.showMessage("Chef has nothing to cook and waits.");
            return;
        }
        while (true) {
            Optional<Order> choice = ui.chooseOrderToCook(restaurant);
            ActionResult result = choice.map(order -> chef.cook(order, turn))
                    .orElse(ActionResult.ok("Chef waits."));
            report(result);
            if (result.success()) {
                return;
            }
        }
    }

    private void report(ActionResult result) {
        ui.showMessage(result.success() ? result.message() : "Not allowed: " + result.message() + ". Choose again.");
        flushLog();
    }

    private void flushLog() {
        restaurant.log().drain().forEach(ui::showMessage);
    }

    private boolean waiterHasWork() {
        return restaurant.canSeatSomeone()
                || !restaurant.seatedWithoutOrder().isEmpty()
                || !restaurant.servableOrders(turn).isEmpty();
    }

    private List<String> availableTasks() {
        List<String> tasks = new ArrayList<>();
        if (restaurant.canSeatSomeone()) {
            tasks.add("Waiter: seat a waiting customer");
            tasks.add("Host: seat a waiting customer");
        }
        for (Customer customer : restaurant.seatedWithoutOrder()) {
            tasks.add("Waiter: take " + customer.id() + "'s order");
        }
        for (Order order : restaurant.servableOrders(turn)) {
            tasks.add("Waiter: serve " + order.describe());
        }
        for (Order order : restaurant.cookableOrders()) {
            tasks.add("Chef: cook " + order.describe() + " (" + order.progress() + ")");
        }
        if (tasks.isEmpty()) {
            tasks.add("None - staff will wait");
        }
        return tasks;
    }
}
```

- [ ] **Step 6: Replace `GameSetup`** — `src/restaurantrush/control/GameSetup.java`

```java
package restaurantrush.control;

import java.util.List;
import restaurantrush.entity.Cashier;
import restaurantrush.entity.Chef;
import restaurantrush.entity.ComboMeal;
import restaurantrush.entity.Dish;
import restaurantrush.entity.HappyHour;
import restaurantrush.entity.Host;
import restaurantrush.entity.Menu;
import restaurantrush.entity.Money;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.TurnLog;
import restaurantrush.entity.Waiter;

/** Builds the base-setting game. Used by Main and by the scripted-run tests. */
public final class GameSetup {
    public static final String EDITION = "Stage 3: Host, combo meals and Happy Hour";

    private GameSetup() {
    }

    /** The menu in rotation order: three dishes, then the three combos. */
    public static Menu baseMenu() {
        Dish salad = new Dish("Salad", Money.ofDollars(9), 1);
        Dish burger = new Dish("Burger", Money.ofDollars(15), 1);
        Dish pasta = new Dish("Pasta", Money.ofDollars(18), 2);
        return new Menu(List.of(salad, burger, pasta,
                new ComboMeal(salad, burger),
                new ComboMeal(salad, pasta),
                new ComboMeal(burger, pasta)));
    }

    public static GameController baseGame(GameUI ui) {
        GameConfig config = GameConfig.base();
        Restaurant restaurant = new Restaurant(baseMenu(), config.tableCount(), new TurnLog(), new HappyHour());
        return new GameController(config, restaurant, ArrivalSchedule.base(),
                new Host(), new Waiter(), new Chef(), new Cashier(), ui);
    }
}
```

- [ ] **Step 7: Add the two prompts to `GameCLI` and the status line to `StatusView`**

In `src/restaurantrush/boundary/GameCLI.java`, add the imports:

```java
import restaurantrush.control.Seating;
import restaurantrush.entity.HappyHour;
```

and insert these methods directly before `chooseWaiterTask`:

```java
    @Override
    public boolean askActivateHappyHour(HappyHour happyHour) {
        out.println("Happy Hour is available (once per game; lasts this turn and the next).");
        out.println("  1) Activate now");
        out.println("  0) Not yet");
        return readChoice("Happy Hour> ", 0, 1) == 1;
    }

    @Override
    public Optional<Seating> chooseHostSeating(Restaurant restaurant) {
        while (true) {
            Optional<Customer> customer = pick("Host, choose a customer to seat (0 = Wait):",
                    restaurant.presentCustomers(), view::describeCustomer);
            if (customer.isEmpty()) {
                return Optional.empty();
            }
            Optional<Table> table = pick("At which table? (0 = back)", restaurant.tables(), view::describeTable);
            if (table.isPresent()) {
                return Optional.of(new Seating(customer.get(), table.get()));
            }
        }
    }
```

In `src/restaurantrush/boundary/StatusView.java`, in `state(...)`, insert directly after the statement that appends the `Revenue:` line:

```java
        text.append("Happy Hour: ").append(restaurant.happyHour().status()).append('\n');
```

- [ ] **Step 8: Replace the test helpers**

`test/restaurantrush/control/ScriptedUI.java`:

```java
package restaurantrush.control;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import restaurantrush.entity.HappyHour;
import restaurantrush.entity.Order;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Scoreboard;

/**
 * A GameUI that replays decisions registered per turn and records what was
 * shown. When no decision is registered for a prompt, the staff member waits
 * and Happy Hour is declined.
 */
class ScriptedUI implements GameUI {
    private final Map<Integer, Deque<Function<Restaurant, Optional<Seating>>>> hostSteps = new HashMap<>();
    private final Map<Integer, Deque<Function<Restaurant, WaiterTask>>> waiterSteps = new HashMap<>();
    private final Map<Integer, Deque<Function<Restaurant, Optional<Order>>>> chefSteps = new HashMap<>();
    private final Set<Integer> happyHourTurns = new HashSet<>();
    final List<String> messages = new ArrayList<>();
    List<String> lastTasks = List.of();
    int turn;
    int happyHourPrompts;
    int hostPrompts;
    int waiterPrompts;
    int chefPrompts;
    Boolean victory;

    ScriptedUI happyHourOn(int turn) {
        happyHourTurns.add(turn);
        return this;
    }

    ScriptedUI hostOn(int turn, Function<Restaurant, Optional<Seating>> step) {
        hostSteps.computeIfAbsent(turn, t -> new ArrayDeque<>()).add(step);
        return this;
    }

    ScriptedUI waiterOn(int turn, Function<Restaurant, WaiterTask> step) {
        waiterSteps.computeIfAbsent(turn, t -> new ArrayDeque<>()).add(step);
        return this;
    }

    ScriptedUI chefOn(int turn, Function<Restaurant, Optional<Order>> step) {
        chefSteps.computeIfAbsent(turn, t -> new ArrayDeque<>()).add(step);
        return this;
    }

    boolean saw(String fragment) {
        return messages.stream().anyMatch(m -> m.contains(fragment));
    }

    @Override
    public void showTurnHeader(int turn, int maxTurns) {
        this.turn = turn;
    }

    @Override
    public void showMessage(String message) {
        messages.add(message);
    }

    @Override
    public void showState(Restaurant restaurant, Scoreboard scoreboard, List<String> availableTasks) {
        lastTasks = availableTasks;
    }

    @Override
    public boolean askActivateHappyHour(HappyHour happyHour) {
        happyHourPrompts++;
        return happyHourTurns.contains(turn);
    }

    @Override
    public Optional<Seating> chooseHostSeating(Restaurant restaurant) {
        hostPrompts++;
        Deque<Function<Restaurant, Optional<Seating>>> steps = hostSteps.get(turn);
        return steps == null || steps.isEmpty() ? Optional.empty() : steps.poll().apply(restaurant);
    }

    @Override
    public WaiterTask chooseWaiterTask(Restaurant restaurant) {
        waiterPrompts++;
        Deque<Function<Restaurant, WaiterTask>> steps = waiterSteps.get(turn);
        return steps == null || steps.isEmpty() ? WaiterTask.waitTurn() : steps.poll().apply(restaurant);
    }

    @Override
    public Optional<Order> chooseOrderToCook(Restaurant restaurant) {
        chefPrompts++;
        Deque<Function<Restaurant, Optional<Order>>> steps = chefSteps.get(turn);
        return steps == null || steps.isEmpty() ? Optional.empty() : steps.poll().apply(restaurant);
    }

    @Override
    public void showTurnEnd(int turn, int maxTurns, Scoreboard scoreboard) {
    }

    @Override
    public void showResult(boolean victory, Scoreboard scoreboard, GameConfig config) {
        this.victory = victory;
    }
}
```

`test/restaurantrush/control/Games.java`:

```java
package restaurantrush.control;

import restaurantrush.entity.Cashier;
import restaurantrush.entity.Chef;
import restaurantrush.entity.Host;
import restaurantrush.entity.Money;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Waiter;

/** Builds controllers for tests, with fresh staff. */
final class Games {
    private Games() {
    }

    /** Base targets and two tables, with a custom game length. */
    static GameConfig config(int maxTurns) {
        return new GameConfig(maxTurns, 2, 4, Money.ofDollars(45), 60);
    }

    static GameController controller(GameConfig config, Restaurant restaurant, ArrivalSchedule schedule, GameUI ui) {
        return new GameController(config, restaurant, schedule, new Host(), new Waiter(), new Chef(), new Cashier(), ui);
    }
}
```

- [ ] **Step 9: Replace `runs/victory.txt`**

```
# Restaurant Rush - victory run (base setting, C2 is a VIP and C3 a Critic)
# Expected: VICTORY - 5 paid, $86.40, average satisfaction 89.4
# Each turn starts with the Happy Hour question while it is unused (0 = not yet).
# Turn 1: Host seats C1 at table 1; Waiter takes C1's order (Salad); Chef cooks it
0
1
1
2
1
1
# Turn 2: serve C1's Salad
0
3
1
# Turn 3: everyone else waits automatically
0
# Turn 4: Host seats C2 (VIP) at table 2; Waiter takes C2's order (Burger, $13.50); Chef cooks it
0
2
2
2
2
1
# Turn 5: serve C2's Burger
0
3
1
# Turn 6: nothing to do
0
# Turn 7: Host seats C3 (Critic) at table 1; Waiter takes C3's order (Pasta); Chef cooks unit 1
0
2
1
2
2
1
# Turn 8: activate Happy Hour so the Critic recovers while the Pasta finishes; Chef cooks unit 2
1
1
# Turn 9: Happy Hour still on; serve the Critic's Pasta fresh
3
1
# Turn 10: Host seats C4 at table 2; Waiter takes C4's order (Salad + Burger); Chef cooks unit 1
2
2
2
2
1
# Turn 11: Chef cooks unit 2
1
# Turn 12: serve C4's combo
3
1
# Turn 13: Host seats C5 at table 1 so the Waiter can take the 3-unit Salad + Pasta order now; Chef cooks unit 1
2
1
2
2
1
# Turn 14: Chef cooks unit 2
1
# Turn 15: Chef cooks unit 3
1
# Turn 16: Host seats C6 at table 2; Waiter serves C5's combo
2
2
3
1
# Turn 17: take C6's order (Burger + Pasta); Chef cooks unit 1
2
2
1
# Turn 18: Chef cooks unit 2 - C6 cannot pay in time
1
```

- [ ] **Step 10: Replace `runs/defeat.txt`**

```
# Restaurant Rush - defeat run (base setting)
# Turn 1 shows invalid input being rejected; after that nobody acts, so
# nobody is served; C1 and the Critic (C3) leave.
# Expected: DEFEAT - 0 paid, $0.00, average N/A, 2 unhappy departures
# Every turn: Happy Hour 0 (not yet), Host 0 (wait), Waiter 0 (wait).
# Turn 1: Happy Hour, Host, then the Waiter gets a word, an out-of-range number, Serve with nothing to serve, then Wait
0
0
abc
9
3
0
# Turn 2
0
0
0
# Turn 3
0
0
0
# Turn 4
0
0
0
# Turn 5
0
0
0
# Turn 6
0
0
0
# Turn 7
0
0
0
# Turn 8
0
0
0
# Turn 9
0
0
0
# Turn 10
0
0
0
# Turn 11
0
0
0
# Turn 12
0
0
0
# Turn 13
0
0
0
# Turn 14
0
0
0
# Turn 15
0
0
0
# Turn 16
0
0
0
# Turn 17
0
0
0
# Turn 18
0
0
0
```

- [ ] **Step 11: Run the tests**

Run: `./test.sh`
Expected: PASS — 0 failures. If a run test fails, replay with `./run.sh --echo < runs/victory.txt` and compare each turn's prompts with the trace table above.

- [ ] **Step 12: Confirm the controller still has no customer type checks**

Run: `grep -rnE "instanceof|VIPCustomer|CriticCustomer" src/restaurantrush/control | grep -v ArrivalSchedule || echo "clean"`
Expected: `clean`.

- [ ] **Step 13: Commit**

```bash
git add src test runs
git commit -m "$(cat <<'EOF'
Add Host, Happy Hour decisions and combo menu to the playable game

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

### Task 16: Stage 3 README and the `stage-3` release

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: the finished Stage 3 game (Tasks 13–15).
- Produces: the `stage-3` tag; README documentation of the 2 → 3 changes.

- [ ] **Step 1: Update `README.md`**

In `## Reproducible runs`, replace the two table rows with:

```markdown
| `victory.txt` | Host seats every arrival so the Waiter can take the order the same turn; Happy Hour on turn 8 keeps the Critic happy while the Pasta finishes; serve READY dishes before anything else | VICTORY: 5 paid, $86.40, average satisfaction 89.4 |
| `defeat.txt` | Shows invalid input being rejected, then nobody acts all game | DEFEAT: 0 paid, $0.00, average N/A, 2 unhappy departures (C1 and the Critic) |
```

In `## How to play`, replace the staff bullet list with:

```markdown
- **Happy Hour** (once per game): at the start of a turn you may switch it on. It lasts that turn and the next.
- **Host** (one task per turn): seat a waiting customer at a free table, in addition to the Waiter's task. `0` waits.
- **Waiter** (one task per turn): seat a waiting customer at a free table, take a seated customer's order, or serve a READY dish. `0` waits.
- **Chef** (one preparation unit per turn): choose an order to advance. `0` waits.
- **Cashier**: automatic. Takes one payment per turn, oldest first.
```

In `## Rules as implemented`, replace the menu table with:

```markdown
| Menu item (rotation order) | Price | Preparation units |
|---|---|---|
| Salad | $9.00 | 1 |
| Burger | $15.00 | 1 |
| Pasta | $18.00 | 2 |
| Salad + Burger combo | $21.60 | 2 |
| Salad + Pasta combo | $24.30 | 3 |
| Burger + Pasta combo | $29.70 | 3 |

A combo's preparation units are the sum of its two items, and its price is 90% of their combined price, rounded to the cent.
```

Replace the `**Food choice:**` paragraph with:

```markdown
**Food choice:** customers choose, never the manager. The rule is rotation by arrival number over the menu above: customer #1 orders the first item, #2 the second, and so on, so in the base setting every customer orders something different (#4 to #6 order the combos). The choice is shown when the order is taken.

**Happy Hour:** can be switched on once per game, at the start of a turn, and lasts that turn and the next; the status line shows how many turns remain. Orders taken while it is active get 20% off, applied after the VIP discount (a VIP Burger costs $15.00 x 0.9 x 0.8 = $10.80); the price is locked when the order is taken. During each active turn, customers still waiting for a table or for food recover 15 satisfaction (maximum 100).

**Host:** seats at most one queued customer per turn, before the Waiter acts, using the same seating rules as the Waiter.
```

Append to the numbered interpretations list:

```markdown
9. Each turn runs: arrivals, state display, the Happy Hour question, Happy Hour recovery (if active), Host, Waiter, Chef, payment, eating, waiting and abandonment, then the turn summary.
10. Happy Hour recovery applies to the same customers who lose satisfaction at the end of the turn: waiting for a table, seated without an order, or ordered but not yet served.
11. Happy Hour may be switched on during turn 18; it then covers only turn 18.
```

In `## Project structure`, replace the `restaurantrush.entity` row's description with:

```markdown
| `restaurantrush.entity` | Domain objects that own the rules: `Restaurant`, the `Customer` hierarchy (Regular, VIP, Critic), `Order`, `Menu`/`MenuItem` with `Dish` and `ComboMeal`, `HappyHour`, `Table`, the `Staff` hierarchy (Host, Waiter, Chef, Cashier), `Money`, `Scoreboard`. |
```

In `## Stages`, add the row:

```markdown
| `stage-3` | Host, ComboMeal and Happy Hour |
```

At the end of `## Changes between stages`, append:

```markdown
### Stage 2 → Stage 3 ([compare](https://github.com/respoopu/SC2002_Lab_Assignment/compare/stage-2...stage-3))

New files:
- `entity/ComboMeal.java`: a `MenuItem` composed of two `MenuItem`s; price and preparation units derive from its parts.
- `entity/HappyHour.java`: the once-per-game promotion (activation, countdown, 20% price adjustment, status text).
- `entity/Host.java`: a `Staff` member that seats one customer per turn via `Restaurant.seat()`.
- `control/Seating.java`: the manager's instruction to the Host.
- Tests: `ComboMealTest`, `HappyHourTest`, `HostTest`.

Changed files:
- `entity/Customer.java`: added `recover(points)`, because Happy Hour raises satisfaction (capped at 100).
- `entity/Restaurant.java`: holds the `HappyHour`; `placeOrder()` applies `happyHour.adjust(...)` after the customer's own discount, which is the moment the price is locked; added `recoverAwaitingCustomers()`. The original three-argument constructor is kept (a restaurant whose Happy Hour is never used), so earlier tests are unchanged.
- `control/GameUI.java`: added `askActivateHappyHour()` and `chooseHostSeating()`; a new staff role and a new manager decision need new questions.
- `control/GameController.java`: added the Happy Hour step and the Host step before the Waiter, and the Happy Hour countdown at the end of the turn. The rest of the turn sequence is unchanged.
- `control/GameSetup.java`: registers the three combos on the menu, creates the Host and the Happy Hour, edition label.
- `boundary/GameCLI.java`: the Happy Hour and Host prompts. `boundary/StatusView.java`: the Happy Hour status line.
- Tests: `ScriptedUI` and `Games` (test helpers) support the Host and Happy Hour; `RunScriptsTest` expects the Stage 3 results; new tests in `RestaurantTest`, `GameControllerTest`, `GameCLITest`, `StatusViewTest`.
- `runs/*.txt`: new answers for the Happy Hour and Host prompts.

Unchanged: the food-choice rule (`Menu.itemFor`), all customer classes except `Customer.recover`, `Order`, `Waiter`, `Chef`, `Cashier`, `ArrivalSchedule`. Combos reach customers through the existing rotation rule with no code change, only menu registration.

Alternative considered: a list of "staff decision phases" that the controller loops over, so the Host would be registered without editing `GameController`. We kept the direct version because it is one addition and easier to read; the trade-off is one controller edit per new role.
```

- [ ] **Step 2: Run the full verification**

Run: `./test.sh && ./run.sh --echo < runs/victory.txt | tail -6 && ./run.sh --echo < runs/defeat.txt | tail -6`
Expected: tests PASS; the victory transcript ends with `GAME OVER: VICTORY`, `Revenue: $86.40` and `Average satisfaction: 89.4`; the defeat transcript ends with `GAME OVER: DEFEAT` and `Unhappy departures: 2`.

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "$(cat <<'EOF'
Document stage 3 rules and the stage 2 to 3 changes

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 4: Push, open and merge the PR**

```bash
git push -u origin feat/stage3-host-combo-happyhour
gh pr create --base main --head feat/stage3-host-combo-happyhour \
  --title "Stage 3: Host, ComboMeal and Happy Hour" \
  --body "$(cat <<'EOF'
## Summary
- Host seats one extra customer per turn before the Waiter acts
- ComboMeal composed of two menu items; three combos join the rotation menu
- Happy Hour: once per game, two turns, 20% off after the VIP discount, +15 recovery for waiting customers
- README documents the 2 -> 3 changes

## Test plan
- [x] `./test.sh` passes, including all Stage 1 and 2 tests
- [x] `./run.sh --echo < runs/victory.txt` ends in VICTORY ($86.40, 89.4)
- [x] `./run.sh --echo < runs/defeat.txt` ends in DEFEAT

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
gh pr merge feat/stage3-host-combo-happyhour --merge
git checkout main
git pull --ff-only origin main
```

- [ ] **Step 5: Tag, verify every tag from a clean export, push the tag**

```bash
git tag -a stage-3 -m "Stage 3: Host, ComboMeal and Happy Hour"
for tag in stage-1 stage-2 stage-3; do
  dir=$(mktemp -d) && git archive "$tag" | tar -x -C "$dir" && "$dir/test.sh" > /dev/null \
    && echo "$tag: $("$dir/run.sh" --echo < "$dir/runs/victory.txt" | grep -E 'GAME OVER|Revenue:' | tr '\n' ' ')"
done
git push origin stage-3
```

Expected:

```
stage-1: ========== GAME OVER: VICTORY ========== Revenue: $66.00 (target $45.00) [met]
stage-2: ========== GAME OVER: VICTORY ========== Revenue: $64.50 (target $45.00) [met]
stage-3: ========== GAME OVER: VICTORY ========== Revenue: $86.40 (target $45.00) [met]
```

---

## Out of scope for this plan

The class and sequence diagrams, the PDF report, the video and the declarations. They must match the finished Stage 3 code, so they follow once `stage-3` is tagged. The spec (§11) lists what they need to cover.
