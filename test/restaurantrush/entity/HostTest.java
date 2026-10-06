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
