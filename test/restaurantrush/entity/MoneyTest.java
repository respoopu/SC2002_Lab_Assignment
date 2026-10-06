package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void formatsAsDollarsWithTwoDecimals() {
        assertEquals("$9.00", Money.ofDollars(9).toString());
        assertEquals("$24.30", new Money(2430).toString());
        assertEquals("$0.05", new Money(5).toString());
    }

    @Test
    void plusAddsAmounts() {
        assertEquals(new Money(2400), Money.ofDollars(9).plus(Money.ofDollars(15)));
    }

    @Test
    void percentOffRoundsHalfUpToTheCent() {
        assertEquals(new Money(1350), Money.ofDollars(15).percentOff(10));
        assertEquals(new Money(501), new Money(1001).percentOff(50));   // 500.5 -> 501
        assertEquals(new Money(899), new Money(999).percentOff(10));    // 899.1 -> 899
        assertEquals(Money.ZERO, Money.ofDollars(3).percentOff(100));
    }

    @Test
    void rejectsNegativeAmountsAndBadPercentages() {
        assertThrows(IllegalArgumentException.class, () -> new Money(-1));
        assertThrows(IllegalArgumentException.class, () -> Money.ofDollars(1).percentOff(101));
    }

    @Test
    void comparesByAmount() {
        assertTrue(Money.ofDollars(45).compareTo(new Money(4499)) > 0);
        assertEquals(0, Money.ofDollars(45).compareTo(new Money(4500)));
    }
}
