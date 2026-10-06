package restaurantrush.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/** The UI is handed the Restaurant to display it; it must not be able to change it behind the staff's back. */
class RestaurantEncapsulationTest {

    @Test
    void stateChangingRulesAreReachableOnlyThroughStaff() throws NoSuchMethodException {
        assertFalse(isPublic(Restaurant.class.getDeclaredMethod("seat", Customer.class, Table.class)));
        assertFalse(isPublic(Restaurant.class.getDeclaredMethod("placeOrder", Customer.class, int.class)));
        assertFalse(isPublic(Restaurant.class.getDeclaredMethod("serve", Order.class, int.class)));
        assertFalse(isPublic(Customer.class.getDeclaredMethod("decay")));
    }

    private static boolean isPublic(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }
}
