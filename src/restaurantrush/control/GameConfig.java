package restaurantrush.control;

import restaurantrush.entity.Money;
import restaurantrush.entity.Scoreboard;

/** Length of the game, number of tables and the three victory targets. */
public record GameConfig(int maxTurns, int tableCount, int targetPaidCustomers,
                         Money targetRevenue, int targetAverageSatisfaction) {

    /** The base setting every run must offer so results can be reproduced. */
    public static GameConfig base() {
        return new GameConfig(18, 2, 4, Money.ofDollars(45), 60);
    }

    public boolean paidTargetMet(int paidCount) {
        return paidCount >= targetPaidCustomers;
    }

    public boolean revenueTargetMet(Money revenue) {
        return revenue.compareTo(targetRevenue) >= 0;
    }

    /** Compares sum >= target x count in integers, so an average of exactly 60 counts. */
    public boolean satisfactionTargetMet(int satisfactionSum, int paidCount) {
        return paidCount > 0 && satisfactionSum >= (long) targetAverageSatisfaction * paidCount;
    }

    public boolean isVictory(Scoreboard scoreboard) {
        return paidTargetMet(scoreboard.paidCount())
                && revenueTargetMet(scoreboard.revenue())
                && satisfactionTargetMet(scoreboard.paidSatisfactionSum(), scoreboard.paidCount());
    }
}
