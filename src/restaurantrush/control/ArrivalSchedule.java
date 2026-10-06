package restaurantrush.control;

import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;
import restaurantrush.entity.CriticCustomer;
import restaurantrush.entity.Customer;
import restaurantrush.entity.RegularCustomer;
import restaurantrush.entity.VIPCustomer;

/** Which kind of customer arrives on which turn. At most one arrival per turn. */
public class ArrivalSchedule {
    private final TreeMap<Integer, BiFunction<Integer, Integer, Customer>> arrivals = new TreeMap<>();

    /**
     * Registers a customer type for a turn. The factory receives the arrival
     * number and the turn, e.g. {@code RegularCustomer::new}.
     */
    public ArrivalSchedule add(int turn, BiFunction<Integer, Integer, Customer> factory) {
        if (arrivals.containsKey(turn)) {
            throw new IllegalArgumentException("Turn " + turn + " already has an arrival");
        }
        arrivals.put(turn, factory);
        return this;
    }

    /** Creates the customer arriving this turn, numbered by arrival order (#1, #2, ...). */
    public Optional<Customer> arrivalAt(int turn) {
        BiFunction<Integer, Integer, Customer> factory = arrivals.get(turn);
        if (factory == null) {
            return Optional.empty();
        }
        int arrivalNo = arrivals.headMap(turn).size() + 1;
        return Optional.of(factory.apply(arrivalNo, turn));
    }

    /**
     * Base setting: one customer at the start of turns 1, 4, 7, 10, 13 and 16.
     * Arrival #2 is a VIP and #3 a Critic; the rest are Regular.
     */
    public static ArrivalSchedule base() {
        return new ArrivalSchedule()
                .add(1, RegularCustomer::new)
                .add(4, VIPCustomer::new)
                .add(7, CriticCustomer::new)
                .add(10, RegularCustomer::new)
                .add(13, RegularCustomer::new)
                .add(16, RegularCustomer::new);
    }
}
