package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CriticCustomerTest {
    private final Dish pasta = new Dish("Pasta", Money.ofDollars(18), 2);
    private final TurnLog log = new TurnLog();

    private Order readyOnTurn(Customer customer, int turn) {
        Order order = new Order(customer, pasta, pasta.price(), turn - 1);
        order.cook(turn - 1);
        order.cook(turn);
        return order;
    }

    @Test
    void losesTwelvePerWaitingTurnAndPaysFullPrice() {
        CriticCustomer critic = new CriticCustomer(3, 7);
        critic.decay();
        assertEquals(88, critic.satisfaction());
        assertEquals(Money.ofDollars(18), critic.priceFor(pasta));
        assertEquals("C3 (Critic)", critic.toString());
    }

    @Test
    void freshFoodBringsNoComplaint() {
        CriticCustomer critic = new CriticCustomer(3, 7);
        critic.onServed(readyOnTurn(critic, 9), 10, log);
        assertEquals(100, critic.satisfaction());
        assertEquals(List.of("C3 (Critic): served fresh - no complaints."), log.drain());
    }

    @Test
    void coldFoodCostsTwentySatisfaction() {
        CriticCustomer critic = new CriticCustomer(3, 7);
        critic.onServed(readyOnTurn(critic, 9), 11, log);
        assertEquals(80, critic.satisfaction());
        assertEquals(List.of("C3 (Critic): cold food! -20 satisfaction (now 80)."), log.drain());
    }

    @Test
    void penaltyStopsAtZero() {
        CriticCustomer critic = new CriticCustomer(3, 7);
        for (int i = 0; i < 8; i++) {
            critic.decay();
        }
        assertEquals(4, critic.satisfaction());
        critic.onServed(readyOnTurn(critic, 9), 12, log);
        assertEquals(0, critic.satisfaction());
    }

    @Test
    void otherCustomersDoNotReactToService() {
        RegularCustomer regular = new RegularCustomer(1, 1);
        regular.onServed(readyOnTurn(regular, 2), 9, log);
        assertEquals(100, regular.satisfaction());
        assertEquals(List.of(), log.drain());
    }
}
