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
