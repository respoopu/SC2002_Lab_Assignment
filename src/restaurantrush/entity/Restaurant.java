package restaurantrush.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The dining room: menu, tables, waiting queue, every customer who has
 * arrived and every order placed. Seating, ordering and serving are
 * validated here, so every staff member who performs them follows the same
 * rules.
 */
public class Restaurant {
    private final Menu menu;
    private final TurnLog log;
    private final List<Table> tables = new ArrayList<>();
    private final List<Customer> waitingQueue = new ArrayList<>();
    private final List<Customer> customers = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();

    public Restaurant(Menu menu, int tableCount, TurnLog log) {
        if (tableCount < 1) {
            throw new IllegalArgumentException("A restaurant needs at least one table");
        }
        this.menu = menu;
        this.log = log;
        for (int number = 1; number <= tableCount; number++) {
            tables.add(new Table(number));
        }
    }

    public Menu menu() {
        return menu;
    }

    public TurnLog log() {
        return log;
    }

    public List<Table> tables() {
        return Collections.unmodifiableList(tables);
    }

    /** Customers waiting for a table, in arrival order. */
    public List<Customer> waitingQueue() {
        return Collections.unmodifiableList(waitingQueue);
    }

    /** Everyone who has arrived, in arrival order. */
    public List<Customer> customers() {
        return Collections.unmodifiableList(customers);
    }

    /** Every order placed, in the order taken. */
    public List<Order> orders() {
        return Collections.unmodifiableList(orders);
    }

    public Table table(int number) {
        if (number < 1 || number > tables.size()) {
            throw new IllegalArgumentException("No table " + number);
        }
        return tables.get(number - 1);
    }

    public Customer customer(String id) {
        return customers.stream()
                .filter(c -> c.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No customer " + id));
    }

    /** Customers still in the restaurant (not paid and not left), in arrival order. */
    public List<Customer> presentCustomers() {
        return customers.stream()
                .filter(c -> c.status() != CustomerStatus.PAID && c.status() != CustomerStatus.LEFT)
                .toList();
    }

    /** Orders still being cooked or waiting to be served, in the order taken. */
    public List<Order> activeOrders() {
        return orders.stream()
                .filter(o -> o.status() == OrderStatus.PLACED || o.status() == OrderStatus.READY)
                .toList();
    }

    public List<Order> cookableOrders() {
        return orders.stream().filter(o -> o.status() == OrderStatus.PLACED).toList();
    }

    public List<Order> servableOrders(int turn) {
        return orders.stream().filter(o -> o.canServe(turn).success()).toList();
    }

    public List<Customer> seatedWithoutOrder() {
        return customers.stream().filter(c -> c.status() == CustomerStatus.SEATED).toList();
    }

    public boolean canSeatSomeone() {
        return !waitingQueue.isEmpty() && tables.stream().anyMatch(Table::isFree);
    }

    public void admit(Customer customer) {
        customers.add(customer);
        waitingQueue.add(customer);
        log.add(customer + " arrives and joins the waiting queue (satisfaction " + customer.satisfaction() + ").");
    }

    ActionResult seat(Customer customer, Table table) {
        if (customer.status() != CustomerStatus.WAITING) {
            return ActionResult.fail(customer.id() + " is not waiting for a table (" + customer.status() + ")");
        }
        if (!table.isFree()) {
            return ActionResult.fail("Table " + table.number() + " is occupied by " + table.occupant().id());
        }
        waitingQueue.remove(customer);
        customer.seatAt(table);
        return ActionResult.ok(customer.id() + " is seated at table " + table.number() + ".");
    }

    /** The customer chooses their dish; the bill is fixed now and never recalculated. */
    ActionResult placeOrder(Customer customer, int turn) {
        if (customer.status() == CustomerStatus.WAITING) {
            return ActionResult.fail(customer.id() + " has not been seated yet");
        }
        if (customer.status() != CustomerStatus.SEATED) {
            return ActionResult.fail(customer.id() + " has already ordered");
        }
        MenuItem item = customer.chooseItem(menu);
        Money price = customer.priceFor(item);
        Order order = new Order(customer, item, price, turn);
        orders.add(order);
        customer.attachOrder(order);
        return ActionResult.ok(customer.id() + " orders " + item + " - bill " + price + ".");
    }

    ActionResult serve(Order order, int turn) {
        ActionResult check = order.canServe(turn);
        if (!check.success()) {
            return check;
        }
        Customer customer = order.customer();
        customer.markServed(turn);
        customer.onServed(order, turn, log);
        return ActionResult.ok(order.describe() + " is SERVED.");
    }

    /** Step 4: customers served on an earlier turn have finished eating. */
    public void finishEating(int turn) {
        for (Customer customer : customers) {
            if (customer.status() == CustomerStatus.SERVED && customer.servedTurn() < turn) {
                customer.markReadyToPay(turn);
                log.add(customer.id() + " finished eating and is READY_TO_PAY.");
            }
        }
    }

    /**
     * Step 5: everyone still waiting for a table or food loses satisfaction.
     * Anyone who reaches 0 leaves without paying. Returns those who left.
     */
    public List<Customer> applyWaitingDecay() {
        List<Customer> departed = new ArrayList<>();
        for (Customer customer : customers) {
            if (customer.isAwaitingService() && customer.decay()) {
                String cancelled = customer.order() == null ? "" : " (order cancelled)";
                waitingQueue.remove(customer);
                customer.leave();
                departed.add(customer);
                log.add(customer.id() + " lost all patience and left without paying" + cancelled + ".");
            }
        }
        return departed;
    }
}
