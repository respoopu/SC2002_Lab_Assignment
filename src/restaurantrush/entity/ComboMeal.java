package restaurantrush.entity;

/**
 * Two existing menu items sold together. Preparation time is the sum of the
 * parts; the price is 90% of their combined price, rounded to the cent.
 */
public class ComboMeal extends MenuItem {
    public static final int DISCOUNT_PERCENT = 10;

    private final MenuItem first;
    private final MenuItem second;

    public ComboMeal(MenuItem first, MenuItem second) {
        super(first.name() + " + " + second.name());
        this.first = first;
        this.second = second;
    }

    public MenuItem first() {
        return first;
    }

    public MenuItem second() {
        return second;
    }

    @Override
    public Money price() {
        return first.price().plus(second.price()).percentOff(DISCOUNT_PERCENT);
    }

    @Override
    public int prepUnits() {
        return first.prepUnits() + second.prepUnits();
    }
}
