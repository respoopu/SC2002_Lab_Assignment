package restaurantrush.entity;

/** Seats a customer, takes an order or serves a READY dish: one task per turn. */
public class Waiter extends Staff {

    public Waiter() {
        super("Waiter");
    }

    public ActionResult seat(Restaurant restaurant, Customer customer, Table table) {
        return perform(() -> restaurant.seat(customer, table));
    }

    public ActionResult takeOrder(Restaurant restaurant, Customer customer, int turn) {
        return perform(() -> restaurant.placeOrder(customer, turn));
    }

    public ActionResult serve(Restaurant restaurant, Order order, int turn) {
        return perform(() -> restaurant.serve(order, turn));
    }
}
