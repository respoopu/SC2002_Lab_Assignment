package restaurantrush.entity;

/** The standard customer: loses 8 satisfaction per waiting turn and pays full price. */
public class RegularCustomer extends Customer {
    public static final int LOSS_PER_TURN = 8;

    public RegularCustomer(int arrivalNo, int arrivalTurn) {
        super(arrivalNo, arrivalTurn);
    }

    @Override
    public int lossPerTurn() {
        return LOSS_PER_TURN;
    }

    @Override
    public String typeName() {
        return "Regular";
    }
}
