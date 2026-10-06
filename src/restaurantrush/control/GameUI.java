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
