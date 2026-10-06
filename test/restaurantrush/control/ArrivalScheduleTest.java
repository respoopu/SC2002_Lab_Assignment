package restaurantrush.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import restaurantrush.entity.Customer;
import restaurantrush.entity.RegularCustomer;

class ArrivalScheduleTest {

    @Test
    void baseScheduleHasSixArrivalsNumberedInOrder() {
        ArrivalSchedule schedule = ArrivalSchedule.base();
        List<Integer> turns = new ArrayList<>();
        List<String> types = new ArrayList<>();
        for (int turn = 1; turn <= 18; turn++) {
            Optional<Customer> arrival = schedule.arrivalAt(turn);
            if (arrival.isPresent()) {
                turns.add(turn);
                types.add(arrival.get().typeName());
                assertEquals(turns.size(), arrival.get().arrivalNo());
                assertEquals(turn, arrival.get().arrivalTurn());
            }
        }
        assertEquals(List.of(1, 4, 7, 10, 13, 16), turns);
        assertEquals(List.of("Regular", "Regular", "Regular", "Regular", "Regular", "Regular"), types);
    }

    @Test
    void turnsWithoutAnArrivalAreEmpty() {
        assertTrue(ArrivalSchedule.base().arrivalAt(2).isEmpty());
    }

    @Test
    void onlyOneArrivalPerTurn() {
        ArrivalSchedule schedule = new ArrivalSchedule().add(1, RegularCustomer::new);
        assertThrows(IllegalArgumentException.class, () -> schedule.add(1, RegularCustomer::new));
    }
}
