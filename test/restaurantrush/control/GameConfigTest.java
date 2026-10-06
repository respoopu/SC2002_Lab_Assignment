package restaurantrush.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import restaurantrush.entity.Money;

class GameConfigTest {
    private final GameConfig config = GameConfig.base();

    @Test
    void baseSettingMatchesTheBrief() {
        assertEquals(18, config.maxTurns());
        assertEquals(2, config.tableCount());
        assertEquals(4, config.targetPaidCustomers());
        assertEquals(Money.ofDollars(45), config.targetRevenue());
        assertEquals(60, config.targetAverageSatisfaction());
    }

    @Test
    void paidTargetNeedsAtLeastFourCustomers() {
        assertTrue(config.paidTargetMet(4));
        assertFalse(config.paidTargetMet(3));
    }

    @Test
    void revenueTargetNeedsAtLeastFortyFiveDollars() {
        assertTrue(config.revenueTargetMet(new Money(4500)));
        assertFalse(config.revenueTargetMet(new Money(4499)));
    }

    @Test
    void averageOfExactlySixtyMeetsTheTarget() {
        assertTrue(config.satisfactionTargetMet(300, 5));
        assertFalse(config.satisfactionTargetMet(299, 5));
        assertFalse(config.satisfactionTargetMet(0, 0));
    }
}
