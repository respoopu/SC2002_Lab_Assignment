package restaurantrush.entity;

/** Where a customer is in the service flow. */
public enum CustomerStatus {
    WAITING,
    SEATED,
    ORDERED,
    SERVED,
    READY_TO_PAY,
    PAID,
    LEFT
}
