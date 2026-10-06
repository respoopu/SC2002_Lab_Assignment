package restaurantrush.entity;

/**
 * A customer moving through WAITING -> SEATED -> ORDERED -> SERVED ->
 * READY_TO_PAY -> PAID, or LEFT if their satisfaction reaches 0 before they
 * are served. Subclasses decide how quickly satisfaction drops and what they
 * are charged; the lifecycle itself is the same for every customer.
 */
public abstract class Customer {
    public static final int MAX_SATISFACTION = 100;

    private final int arrivalNo;
    private final int arrivalTurn;
    private int satisfaction = MAX_SATISFACTION;
    private CustomerStatus status = CustomerStatus.WAITING;
    private Table table;
    private Order order;
    private int servedTurn;
    private int readyToPayTurn;
    private int paidSatisfaction;

    protected Customer(int arrivalNo, int arrivalTurn) {
        this.arrivalNo = arrivalNo;
        this.arrivalTurn = arrivalTurn;
    }

    /** Satisfaction lost for each turn spent waiting for a table or for food. */
    public abstract int lossPerTurn();

    /** Short label for the customer type, e.g. "Regular". */
    public abstract String typeName();

    /** What this customer is charged for an item, before any restaurant promotion. */
    public Money priceFor(MenuItem item) {
        return item.price();
    }

    /** Customers choose their own food, using the menu's rotation rule. */
    public MenuItem chooseItem(Menu menu) {
        return menu.itemFor(arrivalNo);
    }

    /**
     * Called right after this customer is served. Customers who react to the
     * quality of service override this; by default nothing happens.
     */
    public void onServed(Order order, int turn, TurnLog log) {
        // no reaction by default
    }

    public String id() {
        return "C" + arrivalNo;
    }

    public int arrivalNo() {
        return arrivalNo;
    }

    public int arrivalTurn() {
        return arrivalTurn;
    }

    public int satisfaction() {
        return satisfaction;
    }

    public CustomerStatus status() {
        return status;
    }

    /** The table this customer occupies, or null if they have none. */
    public Table table() {
        return table;
    }

    /** This customer's order, or null before they order. */
    public Order order() {
        return order;
    }

    /** The turn this customer was served, or 0 if not yet served. */
    public int servedTurn() {
        return servedTurn;
    }

    /** The turn this customer finished eating, or 0 if they have not. */
    public int readyToPayTurn() {
        return readyToPayTurn;
    }

    /** Satisfaction recorded when this customer paid, or 0 if they have not paid. */
    public int paidSatisfaction() {
        return paidSatisfaction;
    }

    /** True while the customer is still waiting for a table or for food. */
    public boolean isAwaitingService() {
        return status == CustomerStatus.WAITING
                || status == CustomerStatus.SEATED
                || status == CustomerStatus.ORDERED;
    }

    /** Applies one turn of waiting. Returns true if satisfaction has reached 0. */
    boolean decay() {
        reduceSatisfaction(lossPerTurn());
        return satisfaction == 0;
    }

    protected final void reduceSatisfaction(int points) {
        satisfaction = Math.max(0, satisfaction - points);
    }

    void recover(int points) {
        satisfaction = Math.min(MAX_SATISFACTION, satisfaction + points);
    }

    void seatAt(Table table) {
        requireStatus(CustomerStatus.WAITING);
        table.assign(this);
        this.table = table;
        status = CustomerStatus.SEATED;
    }

    void attachOrder(Order order) {
        requireStatus(CustomerStatus.SEATED);
        this.order = order;
        status = CustomerStatus.ORDERED;
    }

    void markServed(int turn) {
        requireStatus(CustomerStatus.ORDERED);
        order.markServed(turn);
        servedTurn = turn;
        status = CustomerStatus.SERVED;
    }

    void markReadyToPay(int turn) {
        requireStatus(CustomerStatus.SERVED);
        readyToPayTurn = turn;
        status = CustomerStatus.READY_TO_PAY;
    }

    void markPaid() {
        requireStatus(CustomerStatus.READY_TO_PAY);
        order.markPaid();
        paidSatisfaction = satisfaction;
        releaseTable();
        status = CustomerStatus.PAID;
    }

    void leave() {
        if (!isAwaitingService()) {
            throw new IllegalStateException(id() + " cannot leave while " + status);
        }
        if (order != null) {
            order.cancel();
        }
        releaseTable();
        status = CustomerStatus.LEFT;
    }

    private void releaseTable() {
        if (table != null) {
            table.release();
            table = null;
        }
    }

    private void requireStatus(CustomerStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(id() + " is " + status + ", expected " + expected);
        }
    }

    @Override
    public String toString() {
        return id() + " (" + typeName() + ")";
    }
}
