package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HappyHourTest {
    private final HappyHour happyHour = new HappyHour();

    @Test
    void startsAvailableAndChangesNoPrices() {
        assertTrue(happyHour.isAvailable());
        assertFalse(happyHour.isActive());
        assertEquals(Money.ofDollars(15), happyHour.adjust(Money.ofDollars(15)));
        assertEquals("available (once per game)", happyHour.status());
    }

    @Test
    void lastsTheActivationTurnAndTheNext() {
        assertTrue(happyHour.activate().success());
        assertEquals("ACTIVE - 2 turns left including this one", happyHour.status());
        happyHour.endTurn();
        assertTrue(happyHour.isActive());
        assertEquals("ACTIVE - 1 turn left including this one", happyHour.status());
        happyHour.endTurn();
        assertFalse(happyHour.isActive());
        assertEquals("used", happyHour.status());
    }

    @Test
    void canOnlyBeActivatedOncePerGame() {
        happyHour.activate();
        happyHour.endTurn();
        happyHour.endTurn();
        assertFalse(happyHour.isAvailable());
        assertEquals("Happy Hour has already been used this game", happyHour.activate().message());
    }

    @Test
    void takesTwentyPercentOffAfterAnyCustomerDiscount() {
        happyHour.activate();
        assertEquals(new Money(1080), happyHour.adjust(Money.ofDollars(15).percentOff(10)));
    }
}
