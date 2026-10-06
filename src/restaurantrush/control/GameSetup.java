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
    public static final String EDITION = "Stage 2: VIP and Critic customers";

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
