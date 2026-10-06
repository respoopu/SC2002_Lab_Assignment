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
