package restaurantrush.control;

import restaurantrush.entity.Customer;
import restaurantrush.entity.Order;
import restaurantrush.entity.Table;

/** The manager's instruction to the Waiter for this turn. Unused fields are null. */
public record WaiterTask(Kind kind, Customer customer, Table table, Order order) {

    public enum Kind { SEAT, TAKE_ORDER, SERVE, WAIT }

    public static WaiterTask seat(Customer customer, Table table) {
        return new WaiterTask(Kind.SEAT, customer, table, null);
    }

    public static WaiterTask takeOrder(Customer customer) {
        return new WaiterTask(Kind.TAKE_ORDER, customer, null, null);
    }

    public static WaiterTask serve(Order order) {
        return new WaiterTask(Kind.SERVE, null, null, order);
    }

    public static WaiterTask waitTurn() {
        return new WaiterTask(Kind.WAIT, null, null, null);
    }
}
