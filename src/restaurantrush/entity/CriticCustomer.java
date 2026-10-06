package restaurantrush.entity;

/**
 * A food critic: impatient (loses 12 per waiting turn), pays full price and
 * judges freshness. A dish can be served the turn after it became READY at
 * the earliest; if it waits any longer it is cold and costs 20 satisfaction.
 */
public class CriticCustomer extends Customer {
    public static final int LOSS_PER_TURN = 12;
    public static final int COLD_FOOD_PENALTY = 20;

    public CriticCustomer(int arrivalNo, int arrivalTurn) {
        super(arrivalNo, arrivalTurn);
    }

    @Override
    public int lossPerTurn() {
        return LOSS_PER_TURN;
    }

    @Override
    public String typeName() {
        return "Critic";
    }

    @Override
    public void onServed(Order order, int turn, TurnLog log) {
        if (turn - order.readyTurn() > 1) {
            reduceSatisfaction(COLD_FOOD_PENALTY);
            log.add(id() + " (Critic): cold food! -" + COLD_FOOD_PENALTY
                    + " satisfaction (now " + satisfaction() + ").");
        } else {
            log.add(id() + " (Critic): served fresh - no complaints.");
        }
    }
}
