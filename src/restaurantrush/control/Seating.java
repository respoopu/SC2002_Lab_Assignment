package restaurantrush.control;

import restaurantrush.entity.Customer;
import restaurantrush.entity.Table;

/** The manager's instruction to the Host: who to seat, and where. */
public record Seating(Customer customer, Table table) {
}
