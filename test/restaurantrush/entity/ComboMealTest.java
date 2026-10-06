package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;

class ComboMealTest {
    private final Dish salad = new Dish("Salad", Money.ofDollars(9), 1);
    private final Dish burger = new Dish("Burger", Money.ofDollars(15), 1);
    private final Dish pasta = new Dish("Pasta", Money.ofDollars(18), 2);

    @Test
    void briefExampleSaladPlusPasta() {
        ComboMeal combo = new ComboMeal(salad, pasta);
        assertEquals("Salad + Pasta", combo.name());
        assertEquals(3, combo.prepUnits());
        assertEquals(new Money(2430), combo.price());
        assertEquals("Salad + Pasta ($24.30, 3 units)", combo.toString());
    }

    @Test
    void everyBaseComboIsNinetyPercentOfItsParts() {
        assertEquals(new Money(2160), new ComboMeal(salad, burger).price());
        assertEquals(new Money(2970), new ComboMeal(burger, pasta).price());
    }

    @Test
    void priceRoundsHalfUpToTheCent() {
        Dish nickel = new Dish("Mint", new Money(5), 1);
        Dish free = new Dish("Water", Money.ZERO, 1);
        assertEquals(new Money(5), new ComboMeal(nickel, free).price()); // 4.5 cents -> 5
    }

    @Test
    void rotationReachesCombosWithoutAnyChangeToTheRule() {
        ComboMeal saladPasta = new ComboMeal(salad, pasta);
        Menu menu = new Menu(List.of(salad, burger, pasta,
                new ComboMeal(salad, burger), saladPasta, new ComboMeal(burger, pasta)));
        assertSame(saladPasta, menu.itemFor(5));
        assertSame(saladPasta, new RegularCustomer(5, 13).chooseItem(menu));
    }
}
