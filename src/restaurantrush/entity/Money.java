package restaurantrush.entity;

/**
 * An amount of money held as integer cents, so discounts never accumulate
 * floating-point error. Displayed as dollars with two decimals.
 */
public record Money(long cents) implements Comparable<Money> {

    public static final Money ZERO = new Money(0);

    public Money {
        if (cents < 0) {
            throw new IllegalArgumentException("Money cannot be negative: " + cents);
        }
    }

    public static Money ofDollars(long dollars) {
        return new Money(dollars * 100);
    }

    public Money plus(Money other) {
        return new Money(cents + other.cents);
    }

    /** This amount reduced by {@code percent}%, rounded half-up to the cent. */
    public Money percentOff(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Percent must be between 0 and 100: " + percent);
        }
        return new Money((cents * (100 - percent) + 50) / 100);
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(cents, other.cents);
    }

    @Override
    public String toString() {
        return String.format("$%d.%02d", cents / 100, cents % 100);
    }
}
