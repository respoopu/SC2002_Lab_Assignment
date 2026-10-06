package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RestaurantTest {
    private final Restaurant restaurant = Dining.restaurant(2);
    private final Customer c1 = new RegularCustomer(1, 1);
    private final Customer c2 = new RegularCustomer(2, 4);

    @Test
    void arrivingCustomerJoinsTheQueueAndIsAnnounced() {
        restaurant.admit(c1);
        assertEquals(List.of(c1), restaurant.waitingQueue());
        assertEquals(List.of("C1 (Regular) arrives and joins the waiting queue (satisfaction 100)."),
                restaurant.log().drain());
    }

    @Test
    void seatsAWaitingCustomerAtAFreeTable() {
        restaurant.admit(c1);
        ActionResult result = restaurant.seat(c1, restaurant.table(1));
        assertTrue(result.success());
        assertEquals("C1 is seated at table 1.", result.message());
        assertEquals(CustomerStatus.SEATED, c1.status());
        assertSame(c1, restaurant.table(1).occupant());
        assertTrue(restaurant.waitingQueue().isEmpty());
    }

    @Test
    void cannotSeatAtAnOccupiedTable() {
        restaurant.admit(c1);
        restaurant.admit(c2);
        restaurant.seat(c1, restaurant.table(1));
        ActionResult result = restaurant.seat(c2, restaurant.table(1));
        assertFalse(result.success());
        assertEquals("Table 1 is occupied by C1", result.message());
        assertEquals(CustomerStatus.WAITING, c2.status());
        assertEquals(List.of(c2), restaurant.waitingQueue());
    }

    @Test
    void cannotSeatSomeoneWhoIsNotWaiting() {
        restaurant.admit(c1);
        restaurant.seat(c1, restaurant.table(1));
        assertEquals("C1 is not waiting for a table (SEATED)", restaurant.seat(c1, restaurant.table(2)).message());
    }

    @Test
    void managerMaySeatAnyQueuedCustomer() {
        restaurant.admit(c1);
        restaurant.admit(c2);
        assertTrue(restaurant.seat(c2, restaurant.table(1)).success());
        assertEquals(List.of(c1), restaurant.waitingQueue());
    }

    @Test
    void orderUsesTheCustomersOwnChoiceAndLocksThePrice() {
        restaurant.admit(c2);
        restaurant.seat(c2, restaurant.table(1));
        ActionResult result = restaurant.placeOrder(c2, 5);
        assertEquals("C2 orders Burger ($15.00, 1 unit) - bill $15.00.", result.message());
        Order order = c2.order();
        assertEquals("Burger", order.item().name());
        assertEquals(Money.ofDollars(15), order.price());
        assertEquals(5, order.placedTurn());
        assertEquals(OrderStatus.PLACED, order.status());
        assertEquals(CustomerStatus.ORDERED, c2.status());
    }

    @Test
    void cannotOrderBeforeBeingSeatedOrTwice() {
        restaurant.admit(c1);
        assertEquals("C1 has not been seated yet", restaurant.placeOrder(c1, 1).message());
        restaurant.seat(c1, restaurant.table(1));
        restaurant.placeOrder(c1, 2);
        assertEquals("C1 has already ordered", restaurant.placeOrder(c1, 3).message());
    }

    @Test
    void serveNeedsADishThatWasReadyBeforeThisTurn() {
        Order order = Dining.seatedWithOrder(restaurant, c1, 1);
        assertEquals("C1's Salad is still cooking (0/1)", restaurant.serve(order, 1).message());
        order.cook(2);
        assertFalse(restaurant.serve(order, 2).success());
        ActionResult result = restaurant.serve(order, 3);
        assertEquals("C1's Salad is SERVED.", result.message());
        assertEquals(CustomerStatus.SERVED, c1.status());
        assertEquals(3, c1.servedTurn());
        assertEquals(OrderStatus.SERVED, order.status());
    }

    @Test
    void customerEatsForOneFullTurnBeforeBeingReadyToPay() {
        Order order = Dining.seatedWithOrder(restaurant, c1, 1);
        Dining.cookFully(order, 1);
        restaurant.serve(order, 2);
        restaurant.finishEating(2);
        assertEquals(CustomerStatus.SERVED, c1.status());
        restaurant.finishEating(3);
        assertEquals(CustomerStatus.READY_TO_PAY, c1.status());
        assertEquals(3, c1.readyToPayTurn());
    }

    @Test
    void onlyCustomersStillAwaitingServiceLoseSatisfaction() {
        Order order = Dining.seatedWithOrder(restaurant, c1, 1);
        Dining.cookFully(order, 1);
        restaurant.serve(order, 2);
        restaurant.admit(c2);
        restaurant.applyWaitingDecay();
        assertEquals(100, c1.satisfaction());
        assertEquals(92, c2.satisfaction());
    }

    @Test
    void customerAtZeroLeavesCancelsTheUnfinishedOrderAndFreesTheTable() {
        Customer c3 = new RegularCustomer(3, 1);
        Order pasta = Dining.seatedWithOrder(restaurant, c3, 1);
        pasta.cook(1);
        for (int i = 0; i < 12; i++) {
            assertTrue(restaurant.applyWaitingDecay().isEmpty());
        }
        restaurant.log().drain();
        assertEquals(List.of(c3), restaurant.applyWaitingDecay());
        assertEquals(CustomerStatus.LEFT, c3.status());
        assertEquals(OrderStatus.CANCELLED, pasta.status());
        assertTrue(restaurant.table(1).isFree());
        assertTrue(restaurant.activeOrders().isEmpty());
        assertTrue(restaurant.presentCustomers().isEmpty());
        assertEquals(List.of("C3 lost all patience and left without paying (order cancelled)."),
                restaurant.log().drain());
    }

    @Test
    void waitingCustomerWhoLeavesIsRemovedFromTheQueue() {
        restaurant.admit(c1);
        for (int i = 0; i < 13; i++) {
            restaurant.applyWaitingDecay();
        }
        assertEquals(CustomerStatus.LEFT, c1.status());
        assertTrue(restaurant.waitingQueue().isEmpty());
        assertFalse(restaurant.canSeatSomeone());
    }

    @Test
    void servingTellsTheCustomerSoTheyCanReact() {
        CriticCustomer critic = new CriticCustomer(3, 1);
        Order pasta = Dining.seatedWithOrder(restaurant, critic, 1);
        Dining.cookFully(pasta, 2);
        restaurant.log().drain();
        restaurant.serve(pasta, 4);
        assertEquals(80, critic.satisfaction());
        assertEquals(List.of("C3 (Critic): cold food! -20 satisfaction (now 80)."), restaurant.log().drain());
    }

    @Test
    void servedCriticAtZeroStaysAndIsNotTreatedAsWaiting() {
        CriticCustomer critic = new CriticCustomer(3, 1);
        Order pasta = Dining.seatedWithOrder(restaurant, critic, 1);
        for (int i = 0; i < 8; i++) {
            restaurant.applyWaitingDecay();
        }
        Dining.cookFully(pasta, 9);
        restaurant.serve(pasta, 12);
        assertEquals(0, critic.satisfaction());
        assertTrue(restaurant.applyWaitingDecay().isEmpty());
        assertEquals(CustomerStatus.SERVED, critic.status());
    }

    @Test
    void happyHourPriceAppliesAfterTheVipDiscountAndIsLocked() {
        HappyHour happyHour = new HappyHour();
        Restaurant promoted = new Restaurant(restaurant.menu(), 2, new TurnLog(), happyHour);
        Customer vip = new VIPCustomer(2, 4);
        promoted.admit(vip);
        promoted.seat(vip, promoted.table(1));
        happyHour.activate();

        ActionResult result = promoted.placeOrder(vip, 4);

        assertEquals("C2 orders Burger ($15.00, 1 unit) - bill $10.80 (Happy Hour price).", result.message());
        happyHour.endTurn();
        happyHour.endTurn();
        assertEquals(new Money(1080), vip.order().price());
    }

    @Test
    void ordersOutsideHappyHourPayTheNormalPrice() {
        Order order = Dining.seatedWithOrder(restaurant, c2, 4);
        assertEquals(Money.ofDollars(15), order.price());
    }

    @Test
    void happyHourRecoveryHelpsOnlyCustomersStillWaitingAndCapsAt100() {
        Customer c3 = new RegularCustomer(3, 1);
        restaurant.admit(c1);
        restaurant.admit(c2);
        restaurant.applyWaitingDecay();
        restaurant.applyWaitingDecay();
        restaurant.seat(c1, restaurant.table(1));
        restaurant.placeOrder(c1, 3);
        Dining.cookFully(c1.order(), 3);
        restaurant.serve(c1.order(), 4);
        restaurant.admit(c3);

        restaurant.recoverAwaitingCustomers(15);

        assertEquals(84, c1.satisfaction());
        assertEquals(99, c2.satisfaction());
        assertEquals(100, c3.satisfaction());
    }
}
