package restaurantrush.entity;

/** A table that holds at most one customer until they pay or leave. */
public class Table {
    private final int number;
    private Customer occupant;

    public Table(int number) {
        this.number = number;
    }

    public int number() {
        return number;
    }

    public boolean isFree() {
        return occupant == null;
    }

    /** The seated customer, or null when the table is free. */
    public Customer occupant() {
        return occupant;
    }

    void assign(Customer customer) {
        if (!isFree()) {
            throw new IllegalStateException("Table " + number + " is occupied");
        }
        occupant = customer;
    }

    void release() {
        occupant = null;
    }
}
