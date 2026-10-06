package restaurantrush.entity;

import java.util.List;

/**
 * The items customers can order, in rotation order. Customers choose by
 * arrival number: arrival #1 orders the first item, #2 the second, and so on,
 * wrapping around when the list runs out.
 */
public class Menu {
    private final List<MenuItem> items;

    public Menu(List<MenuItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("A menu needs at least one item");
        }
        this.items = List.copyOf(items);
    }

    public List<MenuItem> items() {
        return items;
    }

    public MenuItem itemFor(int arrivalNo) {
        if (arrivalNo < 1) {
            throw new IllegalArgumentException("Arrival numbers start at 1: " + arrivalNo);
        }
        return items.get((arrivalNo - 1) % items.size());
    }
}
