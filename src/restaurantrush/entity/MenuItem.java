package restaurantrush.entity;

/** Something a customer can order. */
public abstract class MenuItem {
    private final String name;

    protected MenuItem(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public abstract Money price();

    public abstract int prepUnits();

    @Override
    public String toString() {
        return name + " (" + price() + ", " + prepUnits() + (prepUnits() == 1 ? " unit)" : " units)");
    }
}
