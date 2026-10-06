package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrderTest {
    private final Dish pasta = new Dish("Pasta", Money.ofDollars(18), 2);
    private final Order order = new Order(new RegularCustomer(3, 7), pasta, pasta.price(), 8);

    @Test
    void newOrderIsPlacedWithItsPriceLocked() {
        assertEquals(OrderStatus.PLACED, order.status());
        assertEquals("0/2", order.progress());
        assertEquals(Money.ofDollars(18), order.price());
        assertEquals(8, order.placedTurn());
        assertEquals("C3's Pasta", order.describe());
    }

    @Test
    void lastUnitMakesTheOrderReady() {
        order.cook(8);
        assertEquals(OrderStatus.PLACED, order.status());
        assertEquals("1/2", order.progress());
        order.cook(9);
        assertEquals(OrderStatus.READY, order.status());
        assertEquals(9, order.readyTurn());
    }

    @Test
    void readyOrderCannotBeCookedAgain() {
        order.cook(8);
        order.cook(9);
        assertEquals("C3's Pasta is already READY", order.canCook().message());
        assertThrows(IllegalStateException.class, () -> order.cook(10));
    }

    @Test
    void cannotServeWhileCooking() {
        ActionResult result = order.canServe(8);
        assertFalse(result.success());
        assertEquals("C3's Pasta is still cooking (0/2)", result.message());
    }

    @Test
    void dishReadiedThisTurnIsServedNextTurnAtTheEarliest() {
        order.cook(8);
        order.cook(9);
        assertEquals("C3's Pasta only became READY this turn; serve it next turn", order.canServe(9).message());
        assertTrue(order.canServe(10).success());
    }

    @Test
    void cancelledOrderCannotBeCookedOrServed() {
        order.cook(8);
        order.cancel();
        assertEquals(OrderStatus.CANCELLED, order.status());
        assertEquals("C3's Pasta was cancelled", order.canCook().message());
        assertFalse(order.canServe(9).success());
    }
}
