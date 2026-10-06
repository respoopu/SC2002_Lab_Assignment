package restaurantrush.boundary;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;
import restaurantrush.control.GameConfig;
import restaurantrush.control.GameUI;
import restaurantrush.control.Seating;
import restaurantrush.control.WaiterTask;
import restaurantrush.entity.Customer;
import restaurantrush.entity.HappyHour;
import restaurantrush.entity.Order;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Scoreboard;
import restaurantrush.entity.Table;

/**
 * Console implementation of {@link GameUI}. Shows the state, reads numbered
 * choices and re-prompts on malformed input. Whether a choice is allowed by
 * the game rules is decided by the controller and the domain objects, not here.
 */
public class GameCLI implements GameUI {
    private final Scanner in;
    private final PrintStream out;
    private final boolean echoInput;
    private final StatusView view = new StatusView();

    /**
     * @param echoInput print each answer after its prompt; used for scripted
     *                  runs, where redirected input does not appear on screen
     */
    public GameCLI(InputStream in, PrintStream out, boolean echoInput) {
        this.in = new Scanner(in);
        this.out = out;
        this.echoInput = echoInput;
    }

    @Override
    public void showTurnHeader(int turn, int maxTurns) {
        out.println(view.header(turn, maxTurns));
    }

    @Override
    public void showMessage(String message) {
        out.println(message);
    }

    @Override
    public void showState(Restaurant restaurant, Scoreboard scoreboard, List<String> availableTasks) {
        out.println(view.state(restaurant, scoreboard, availableTasks));
    }

    @Override
    public boolean askActivateHappyHour(HappyHour happyHour) {
        out.println("Happy Hour is available (once per game; lasts this turn and the next).");
        out.println("  1) Activate now");
        out.println("  0) Not yet");
        return readChoice("Happy Hour> ", 0, 1) == 1;
    }

    @Override
    public Optional<Seating> chooseHostSeating(Restaurant restaurant) {
        while (true) {
            Optional<Customer> customer = pick("Host, choose a customer to seat (0 = Wait):",
                    restaurant.presentCustomers(), view::describeCustomer);
            if (customer.isEmpty()) {
                return Optional.empty();
            }
            Optional<Table> table = pick("At which table? (0 = back)", restaurant.tables(), view::describeTable);
            if (table.isPresent()) {
                return Optional.of(new Seating(customer.get(), table.get()));
            }
        }
    }

    @Override
    public WaiterTask chooseWaiterTask(Restaurant restaurant) {
        while (true) {
            out.println("Waiter, choose a task:");
            out.println("  1) Seat a waiting customer");
            out.println("  2) Take an order");
            out.println("  3) Serve a READY dish");
            out.println("  0) Wait");
            int choice = readChoice("Waiter> ", 0, 3);
            if (choice == 0) {
                return WaiterTask.waitTurn();
            }
            if (choice == 1) {
                Optional<Customer> customer = pick("Seat which customer? (0 = back)",
                        restaurant.presentCustomers(), view::describeCustomer);
                if (customer.isEmpty()) {
                    continue;
                }
                Optional<Table> table = pick("At which table? (0 = back)", restaurant.tables(), view::describeTable);
                if (table.isPresent()) {
                    return WaiterTask.seat(customer.get(), table.get());
                }
            } else if (choice == 2) {
                Optional<Customer> customer = pick("Take whose order? (0 = back)",
                        restaurant.presentCustomers(), view::describeCustomer);
                if (customer.isPresent()) {
                    return WaiterTask.takeOrder(customer.get());
                }
            } else {
                Optional<Order> order = pick("Serve which dish? (0 = back)",
                        restaurant.activeOrders(), view::describeOrder);
                if (order.isPresent()) {
                    return WaiterTask.serve(order.get());
                }
            }
        }
    }

    @Override
    public Optional<Order> chooseOrderToCook(Restaurant restaurant) {
        return pick("Chef, choose an order to cook (0 = Wait):", restaurant.activeOrders(), view::describeOrder);
    }

    @Override
    public void showTurnEnd(int turn, int maxTurns, Scoreboard scoreboard) {
        out.println(view.turnEnd(turn, maxTurns, scoreboard));
    }

    @Override
    public void showResult(boolean victory, Scoreboard scoreboard, GameConfig config) {
        out.println(view.result(victory, scoreboard, config));
    }

    private <T> Optional<T> pick(String question, List<T> options, Function<T, String> label) {
        if (options.isEmpty()) {
            out.println("There is nothing to choose from.");
            return Optional.empty();
        }
        out.println(question);
        for (int i = 0; i < options.size(); i++) {
            out.println("  " + (i + 1) + ") " + label.apply(options.get(i)));
        }
        int choice = readChoice("> ", 0, options.size());
        return choice == 0 ? Optional.empty() : Optional.of(options.get(choice - 1));
    }

    private int readChoice(String prompt, int min, int max) {
        while (true) {
            out.print(prompt);
            out.flush();
            String line = nextAnswer();
            if (echoInput) {
                out.println(line);
            }
            try {
                int value = Integer.parseInt(line.trim());
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // reported below, together with out-of-range numbers
            }
            out.println("Invalid input '" + line.trim() + "': enter a number from " + min + " to " + max + ".");
        }
    }

    /** The next answer line, skipping '#' lines that annotate scripted runs. */
    private String nextAnswer() {
        while (in.hasNextLine()) {
            String line = in.nextLine();
            if (!line.trim().startsWith("#")) {
                return line;
            }
        }
        throw new InputEndedException();
    }
}
