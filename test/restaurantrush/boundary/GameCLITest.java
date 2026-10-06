package restaurantrush.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import restaurantrush.control.GameSetup;
import restaurantrush.control.WaiterTask;
import restaurantrush.entity.Order;
import restaurantrush.entity.RegularCustomer;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.TurnLog;

class GameCLITest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        restaurant = new Restaurant(GameSetup.baseMenu(), 2, new TurnLog());
        restaurant.admit(new RegularCustomer(1, 1));
        restaurant.admit(new RegularCustomer(2, 1));
    }

    private GameCLI cli(String input, boolean echo) {
        return new GameCLI(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8), echo);
    }

    private GameCLI cli(String input) {
        return cli(input, false);
    }

    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void seatChoiceNamesTheCustomerAndTable() {
        WaiterTask task = cli("1\n2\n1\n").chooseWaiterTask(restaurant);
        assertEquals(WaiterTask.Kind.SEAT, task.kind());
        assertEquals("C2", task.customer().id());
        assertEquals(1, task.table().number());
    }

    @Test
    void malformedInputIsExplainedAndAskedAgain() {
        WaiterTask task = cli("abc\n\n2.5\n7\n 0 \n").chooseWaiterTask(restaurant);
        assertEquals(WaiterTask.Kind.WAIT, task.kind());
        String out = printed();
        assertTrue(out.contains("Invalid input 'abc': enter a number from 0 to 3."), out);
        assertTrue(out.contains("Invalid input '': enter a number from 0 to 3."), out);
        assertTrue(out.contains("Invalid input '2.5'"), out);
        assertTrue(out.contains("Invalid input '7'"), out);
    }

    @Test
    void zeroInATargetListGoesBackToTheTaskMenu() {
        assertEquals(WaiterTask.Kind.WAIT, cli("2\n0\n0\n").chooseWaiterTask(restaurant).kind());
    }

    @Test
    void emptyTargetListSaysSoAndGoesBack() {
        assertEquals(WaiterTask.Kind.WAIT, cli("3\n0\n").chooseWaiterTask(restaurant).kind());
        assertTrue(printed().contains("There is nothing to choose from."));
    }

    @Test
    void commentLinesAreSkipped() {
        assertEquals(WaiterTask.Kind.WAIT, cli("# turn 1 note\n0\n").chooseWaiterTask(restaurant).kind());
    }

    @Test
    void chefChoosesFromActiveOrdersOrWaits() {
        restaurant.seat(restaurant.customer("C1"), restaurant.table(1));
        restaurant.placeOrder(restaurant.customer("C1"), 1);
        Optional<Order> order = cli("1\n").chooseOrderToCook(restaurant);
        assertEquals("C1's Salad", order.orElseThrow().describe());
        assertEquals(Optional.empty(), cli("0\n").chooseOrderToCook(restaurant));
    }

    @Test
    void endOfInputIsSignalled() {
        assertThrows(InputEndedException.class, () -> cli("").chooseWaiterTask(restaurant));
    }

    @Test
    void echoModePrintsEachAnswerAfterItsPrompt() {
        cli("0\n", true).chooseWaiterTask(restaurant);
        assertTrue(printed().contains("Waiter> 0"));
    }
}
