package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VIPCustomerTest {
    private final Dish burger = new Dish("Burger", Money.ofDollars(15), 1);

    @Test
    void losesFivePerWaitingTurn() {
        VIPCustomer vip = new VIPCustomer(2, 4);
        vip.decay();
        assertEquals(95, vip.satisfaction());
        assertEquals("C2 (VIP)", vip.toString());
    }

    @Test
    void paysTenPercentLess() {
        assertEquals(new Money(1350), new VIPCustomer(2, 4).priceFor(burger));
    }

    @Test
    void discountIsLockedIntoTheOrder() {
        Restaurant restaurant = Dining.restaurant(2);
        Order order = Dining.seatedWithOrder(restaurant, new VIPCustomer(2, 4), 4);
        assertEquals("Burger", order.item().name());
        assertEquals(new Money(1350), order.price());
    }
}
