package restaurantrush.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.Dish;
import restaurantrush.entity.Menu;
import restaurantrush.entity.Money;
import restaurantrush.entity.OrderStatus;
import restaurantrush.entity.RegularCustomer;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.TurnLog;

class GameControllerTest {
    private final Dish salad = new Dish("Salad", Money.ofDollars(9), 1);
    private final Dish pasta = new Dish("Pasta", Money.ofDollars(18), 2);

    private Restaurant restaurantServing(Dish dish) {
        return new Restaurant(new Menu(List.of(dish)), 2, new TurnLog());
    }

    private ArrivalSchedule oneRegularOnTurnOne() {
        return new ArrivalSchedule().add(1, RegularCustomer::new);
    }

    @Test
    void briefPastaExampleMatchesTurnByTurn() {
        Restaurant restaurant = restaurantServing(pasta);
        ScriptedUI ui = new ScriptedUI()
                .waiterOn(1, r -> WaiterTask.seat(r.customer("C1"), r.table(1)))
                .waiterOn(2, r -> WaiterTask.takeOrder(r.customer("C1")))
                .chefOn(2, r -> Optional.of(r.customer("C1").order()))
                .chefOn(3, r -> Optional.of(r.customer("C1").order()))
                .waiterOn(4, r -> WaiterTask.serve(r.customer("C1").order()));
        GameController game = Games.controller(Games.config(6), restaurant, oneRegularOnTurnOne(), ui);

        game.playTurn(1);
        Customer c1 = restaurant.customer("C1");
        assertEquals(CustomerStatus.SEATED, c1.status());
        assertEquals(92, c1.satisfaction());

        game.playTurn(2);
        assertEquals(CustomerStatus.ORDERED, c1.status());
        assertEquals("1/2", c1.order().progress());
        assertEquals(84, c1.satisfaction());

        game.playTurn(3);
        assertEquals(OrderStatus.READY, c1.order().status());
        assertEquals(76, c1.satisfaction());

        game.playTurn(4);
        assertEquals(CustomerStatus.SERVED, c1.status());
        assertEquals(76, c1.satisfaction());

        game.playTurn(5);
        assertEquals(CustomerStatus.READY_TO_PAY, c1.status());

        game.playTurn(6);
        assertEquals(CustomerStatus.PAID, c1.status());
        assertEquals(76, c1.paidSatisfaction());
        assertEquals(Money.ofDollars(18), game.scoreboard().revenue());
        assertTrue(restaurant.table(1).isFree());
    }

    @Test
    void rejectedChoiceExplainsWhyAndAsksAgainWithoutUsingTheTask() {
        Restaurant restaurant = restaurantServing(salad);
        ScriptedUI ui = new ScriptedUI()
                .waiterOn(1, r -> WaiterTask.takeOrder(r.customer("C1")))
                .waiterOn(1, r -> WaiterTask.seat(r.customer("C1"), r.table(1)));
        GameController game = Games.controller(Games.config(1), restaurant, oneRegularOnTurnOne(), ui);

        game.playTurn(1);

        assertEquals(2, ui.waiterPrompts);
        assertTrue(ui.saw("Not allowed: C1 has not been seated yet. Choose again."));
        assertEquals(CustomerStatus.SEATED, restaurant.customer("C1").status());
    }

    @Test
    void staffWithNothingToDoWaitWithoutBeingAsked() {
        ScriptedUI ui = new ScriptedUI();
        GameController game = Games.controller(Games.config(1), restaurantServing(salad), new ArrivalSchedule(), ui);

        game.playTurn(1);

        assertEquals(0, ui.waiterPrompts);
        assertEquals(0, ui.chefPrompts);
        assertTrue(ui.saw("Waiter has nothing to do this turn and waits."));
        assertTrue(ui.saw("Chef has nothing to cook and waits."));
    }

    @Test
    void startOfTurnDisplayListsTheTasksStaffCanDo() {
        ScriptedUI ui = new ScriptedUI();
        Games.controller(Games.config(1), restaurantServing(salad), oneRegularOnTurnOne(), ui).playTurn(1);

        assertTrue(ui.lastTasks.contains("Waiter: seat a waiting customer"));
        assertTrue(ui.lastTasks.stream().noneMatch(task -> task.startsWith("Chef")));
    }

    @Test
    void neglectedCustomerLeavesWithoutPayingAndTheirOrderIsCancelled() {
        Restaurant restaurant = restaurantServing(pasta);
        ScriptedUI ui = new ScriptedUI()
                .waiterOn(1, r -> WaiterTask.seat(r.customer("C1"), r.table(1)))
                .waiterOn(2, r -> WaiterTask.takeOrder(r.customer("C1")))
                .chefOn(2, r -> Optional.of(r.customer("C1").order()));
        GameController game = Games.controller(Games.config(13), restaurant, oneRegularOnTurnOne(), ui);

        boolean victory = game.run();

        Customer c1 = restaurant.customer("C1");
        assertFalse(victory);
        assertEquals(Boolean.FALSE, ui.victory);
        assertEquals(CustomerStatus.LEFT, c1.status());
        assertEquals(OrderStatus.CANCELLED, c1.order().status());
        assertTrue(restaurant.table(1).isFree());
        assertEquals(1, game.scoreboard().unhappyDepartures());
        assertEquals(Money.ZERO, game.scoreboard().revenue());
        assertTrue(ui.saw("C1 lost all patience and left without paying (order cancelled)."));
    }
}
