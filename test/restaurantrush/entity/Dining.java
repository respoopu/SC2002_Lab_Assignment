package restaurantrush.entity;

import java.util.List;

/** Test helper that walks customers through the service flow quickly. */
final class Dining {
    private Dining() {
    }

    /** A restaurant with the base menu (Salad, Burger, Pasta in rotation order). */
    static Restaurant restaurant(int tables) {
        Menu menu = new Menu(List.of(
                new Dish("Salad", Money.ofDollars(9), 1),
                new Dish("Burger", Money.ofDollars(15), 1),
                new Dish("Pasta", Money.ofDollars(18), 2)));
        return new Restaurant(menu, tables, new TurnLog());
    }

    /** Admits the customer, seats them at the first free table and takes their order on {@code turn}. */
    static Order seatedWithOrder(Restaurant restaurant, Customer customer, int turn) {
        restaurant.admit(customer);
        Table free = restaurant.tables().stream().filter(Table::isFree).findFirst().orElseThrow();
        restaurant.seat(customer, free);
        restaurant.placeOrder(customer, turn);
        return customer.order();
    }

    /** Cooks every remaining unit on {@code turn}, ignoring the Chef's one-unit limit. */
    static void cookFully(Order order, int turn) {
        while (order.status() == OrderStatus.PLACED) {
            order.cook(turn);
        }
    }

    /** Ordered and cooked on {@code turn}, served on turn + 1, READY_TO_PAY on turn + 2. */
    static void readyToPay(Restaurant restaurant, Customer customer, int turn) {
        Order order = seatedWithOrder(restaurant, customer, turn);
        cookFully(order, turn);
        restaurant.serve(order, turn + 1);
        restaurant.finishEating(turn + 2);
    }
}
