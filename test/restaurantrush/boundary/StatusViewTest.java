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
import restaurantrush.entity.Waiter;

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
        Waiter waiter = new Waiter();
        waiter.seat(restaurant, c3, restaurant.table(2));
        waiter.startTurn();
        waiter.takeOrder(restaurant, c3, 1);
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

    @Test
    void stateShowsHappyHourStatus() {
        String state = view.state(restaurant, scoreboard, List.of());
        assertTrue(state.contains("Happy Hour: available (once per game)"), state);
    }
}
