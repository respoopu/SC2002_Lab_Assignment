package restaurantrush.entity;

/** Seats at most one queued customer per turn, in addition to the Waiter's task. */
public class Host extends Staff {

    public Host() {
        super("Host");
    }

    /** Uses the same seating rules as the Waiter, so the two can never disagree. */
    public ActionResult seat(Restaurant restaurant, Customer customer, Table table) {
        return perform(() -> restaurant.seat(customer, table));
    }
}
