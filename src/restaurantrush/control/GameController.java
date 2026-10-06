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
