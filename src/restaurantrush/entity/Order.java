package restaurantrush.entity;

/**
 * One customer's order: what they chose, the bill locked in when the order
 * was taken, and the kitchen's progress. Guards its own status changes.
 */
public class Order {
    private final Customer customer;
    private final MenuItem item;
    private final Money price;
    private final int placedTurn;
    private int unitsDone;
    private OrderStatus status = OrderStatus.PLACED;
    private int readyTurn;
    private int servedTurn;

    Order(Customer customer, MenuItem item, Money price, int placedTurn) {
        this.customer = customer;
        this.item = item;
        this.price = price;
        this.placedTurn = placedTurn;
    }

    public Customer customer() {
        return customer;
    }

    public MenuItem item() {
        return item;
    }

    /** The bill, fixed when the order was taken. */
    public Money price() {
        return price;
    }

    public int placedTurn() {
        return placedTurn;
    }

    public int unitsDone() {
        return unitsDone;
    }

    public OrderStatus status() {
        return status;
    }

    /** The turn the last preparation unit was done, or 0 if not READY yet. */
    public int readyTurn() {
        return readyTurn;
    }

    /** The turn the dish was served, or 0 if not served yet. */
    public int servedTurn() {
        return servedTurn;
    }

    public String progress() {
        return unitsDone + "/" + item.prepUnits();
    }

    public String describe() {
        return customer.id() + "'s " + item.name();
    }

    public ActionResult canCook() {
        switch (status) {
            case PLACED:
                return ActionResult.ok(describe() + " can be cooked");
            case READY:
                return ActionResult.fail(describe() + " is already READY");
            case CANCELLED:
                return ActionResult.fail(describe() + " was cancelled");
            default:
                return ActionResult.fail(describe() + " has already been served");
        }
    }

    /** A dish can be served only if it was already READY before this turn. */
    public ActionResult canServe(int turn) {
        switch (status) {
            case PLACED:
                return ActionResult.fail(describe() + " is still cooking (" + progress() + ")");
            case READY:
                return readyTurn < turn
                        ? ActionResult.ok(describe() + " can be served")
                        : ActionResult.fail(describe() + " only became READY this turn; serve it next turn");
            case CANCELLED:
                return ActionResult.fail(describe() + " was cancelled");
            default:
                return ActionResult.fail(describe() + " has already been served");
        }
    }

    void cook(int turn) {
        ActionResult check = canCook();
        if (!check.success()) {
            throw new IllegalStateException(check.message());
        }
        unitsDone++;
        if (unitsDone == item.prepUnits()) {
            status = OrderStatus.READY;
            readyTurn = turn;
        }
    }

    void markServed(int turn) {
        ActionResult check = canServe(turn);
        if (!check.success()) {
            throw new IllegalStateException(check.message());
        }
        status = OrderStatus.SERVED;
        servedTurn = turn;
    }

    void markPaid() {
        if (status != OrderStatus.SERVED) {
            throw new IllegalStateException(describe() + " cannot be paid while " + status);
        }
        status = OrderStatus.PAID;
    }

    void cancel() {
        if (status == OrderStatus.SERVED || status == OrderStatus.PAID) {
            throw new IllegalStateException(describe() + " has already been served");
        }
        status = OrderStatus.CANCELLED;
    }
}
