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
