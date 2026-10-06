package restaurantrush.entity;

/** A valued guest: more patient (loses 5 per waiting turn) and pays 10% less. */
public class VIPCustomer extends Customer {
    public static final int LOSS_PER_TURN = 5;
    public static final int DISCOUNT_PERCENT = 10;

    public VIPCustomer(int arrivalNo, int arrivalTurn) {
        super(arrivalNo, arrivalTurn);
    }

    @Override
    public int lossPerTurn() {
        return LOSS_PER_TURN;
    }

    @Override
    public String typeName() {
        return "VIP";
    }

    @Override
    public Money priceFor(MenuItem item) {
        return item.price().percentOff(DISCOUNT_PERCENT);
    }
}
