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
