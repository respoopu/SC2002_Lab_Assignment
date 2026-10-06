package restaurantrush.entity;

import java.util.OptionalDouble;

/**
 * Game statistics, computed from the restaurant's current state so they can
 * never drift out of sync with it. Revenue counts only PAID orders, so unpaid
 * and abandoned orders are never included.
 */
public class Scoreboard {
    private final Restaurant restaurant;

    public Scoreboard(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public Money revenue() {
        return restaurant.orders().stream()
                .filter(o -> o.status() == OrderStatus.PAID)
                .map(Order::price)
                .reduce(Money.ZERO, Money::plus);
    }

    public int paidCount() {
        return count(CustomerStatus.PAID);
    }

    /** Sum of the satisfaction each paid customer had when they paid. */
    public int paidSatisfactionSum() {
        return restaurant.customers().stream()
                .filter(c -> c.status() == CustomerStatus.PAID)
                .mapToInt(Customer::paidSatisfaction)
                .sum();
    }

    /** Average satisfaction of paid customers, or empty if nobody has paid. */
    public OptionalDouble averageSatisfaction() {
        int paid = paidCount();
        return paid == 0 ? OptionalDouble.empty() : OptionalDouble.of((double) paidSatisfactionSum() / paid);
    }

    /** Customers who have been served their dish, including those who have since paid. */
    public int servedCount() {
        return (int) restaurant.customers().stream().filter(c -> c.servedTurn() > 0).count();
    }

    public int unhappyDepartures() {
        return count(CustomerStatus.LEFT);
    }

    private int count(CustomerStatus status) {
        return (int) restaurant.customers().stream().filter(c -> c.status() == status).count();
    }
}
