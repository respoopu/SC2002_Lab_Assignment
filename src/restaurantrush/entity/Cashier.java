package restaurantrush.entity;

import java.util.Comparator;
import java.util.Optional;

/** Takes payment automatically: at most one customer per turn, oldest first. */
public class Cashier extends Staff {

    public Cashier() {
        super("Cashier");
    }

    /**
     * Charges the customer who has been READY_TO_PAY the longest (ties go to
     * the earlier arrival). A customer who became ready this turn pays next
     * turn at the earliest. Returns the customer who paid, if any.
     */
    public Optional<Customer> collectPayment(Restaurant restaurant, int turn) {
        Optional<Customer> next = restaurant.customers().stream()
                .filter(c -> c.status() == CustomerStatus.READY_TO_PAY && c.readyToPayTurn() < turn)
                .min(Comparator.comparingInt(Customer::readyToPayTurn).thenComparingInt(Customer::arrivalNo));
        if (next.isEmpty()) {
            return Optional.empty();
        }
        Customer customer = next.get();
        ActionResult result = perform(() -> {
            Money bill = customer.order().price();
            customer.markPaid();
            restaurant.log().add("Cashier collects " + bill + " from " + customer.id()
                    + " (satisfaction " + customer.paidSatisfaction() + " recorded); their table is free.");
            return ActionResult.ok(customer.id() + " paid " + bill);
        });
        return result.success() ? next : Optional.empty();
    }
}
