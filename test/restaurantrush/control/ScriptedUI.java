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
