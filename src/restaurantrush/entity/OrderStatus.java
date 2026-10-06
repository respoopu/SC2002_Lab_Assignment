package restaurantrush.entity;

/** Where an order is between the kitchen and the Cashier. */
public enum OrderStatus {
    PLACED,
    READY,
    SERVED,
    PAID,
    CANCELLED
}
