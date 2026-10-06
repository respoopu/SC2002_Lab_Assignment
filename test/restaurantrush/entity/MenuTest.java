package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class MenuTest {
    private final Dish salad = new Dish("Salad", Money.ofDollars(9), 1);
    private final Dish burger = new Dish("Burger", Money.ofDollars(15), 1);
    private final Dish pasta = new Dish("Pasta", Money.ofDollars(18), 2);
    private final Menu menu = new Menu(List.of(salad, burger, pasta));

    @Test
    void rotationFollowsArrivalOrderAndWrapsAround() {
        assertSame(salad, menu.itemFor(1));
        assertSame(burger, menu.itemFor(2));
        assertSame(pasta, menu.itemFor(3));
        assertSame(salad, menu.itemFor(4));
        assertSame(burger, menu.itemFor(5));
        assertSame(pasta, menu.itemFor(6));
    }

    @Test
    void arrivalNumbersStartAtOne() {
        assertThrows(IllegalArgumentException.class, () -> menu.itemFor(0));
    }

    @Test
    void menuNeedsAtLeastOneItem() {
        assertThrows(IllegalArgumentException.class, () -> new Menu(List.of()));
    }

    @Test
    void dishDescribesItself() {
        assertEquals("Pasta ($18.00, 2 units)", pasta.toString());
        assertEquals("Salad ($9.00, 1 unit)", salad.toString());
    }

    @Test
    void dishNeedsAtLeastOnePreparationUnit() {
        assertThrows(IllegalArgumentException.class, () -> new Dish("Air", Money.ZERO, 0));
    }
}
