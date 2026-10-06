package restaurantrush.boundary;

import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import restaurantrush.control.GameConfig;
import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.Order;
import restaurantrush.entity.OrderStatus;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Scoreboard;
import restaurantrush.entity.Table;

/** Turns game state into text for the console. Contains no game rules. */
public class StatusView {

    public String header(int turn, int maxTurns) {
        return "\n========== Turn " + turn + " of " + maxTurns + " ==========";
    }

    public String state(Restaurant restaurant, Scoreboard scoreboard, List<String> availableTasks) {
        StringBuilder text = new StringBuilder();
        text.append("Revenue: ").append(scoreboard.revenue())
                .append(" | Avg satisfaction (paid): ").append(average(scoreboard))
                .append(" | Paid customers: ").append(scoreboard.paidCount()).append('\n');
        text.append("Happy Hour: ").append(restaurant.happyHour().status()).append('\n');
        text.append("Waiting queue: ").append(queue(restaurant.waitingQueue())).append('\n');
        text.append("Tables: ").append(restaurant.tables().stream()
                .map(this::describeTable).collect(Collectors.joining(" | "))).append('\n');
        text.append("Customers:\n");
        List<Customer> present = restaurant.presentCustomers();
        if (present.isEmpty()) {
            text.append("  (none)\n");
        }
        for (Customer customer : present) {
            text.append("  ").append(describeCustomer(customer)).append('\n');
        }
        text.append("Staff tasks available:");
        for (String task : availableTasks) {
            text.append("\n  - ").append(task);
        }
        return text.toString();
    }

    public String describeCustomer(Customer customer) {
        String where = customer.table() == null ? "" : " at table " + customer.table().number();
        return customer + " - " + statusLabel(customer.status()) + where
                + ", satisfaction " + customer.satisfaction() + ", " + orderLabel(customer.order());
    }

    public String describeOrder(Order order) {
        return order.describe() + " (" + order.price() + "): " + orderStatusLabel(order);
    }

    public String describeTable(Table table) {
        return "Table " + table.number() + (table.isFree() ? " - free" : " - occupied by " + table.occupant().id());
    }

    public String turnEnd(int turn, int maxTurns, Scoreboard scoreboard) {
        return "End of turn " + turn + ": served " + scoreboard.servedCount()
                + " | unhappy departures " + scoreboard.unhappyDepartures()
                + " | revenue " + scoreboard.revenue()
                + " | avg satisfaction " + average(scoreboard)
                + " | turns used " + turn + "/" + maxTurns;
    }

    public String result(boolean victory, Scoreboard scoreboard, GameConfig config) {
        return String.join("\n",
                "",
                "========== GAME OVER: " + (victory ? "VICTORY" : "DEFEAT") + " ==========",
                "Paid customers: " + scoreboard.paidCount() + " (target " + config.targetPaidCustomers() + ") "
                        + mark(config.paidTargetMet(scoreboard.paidCount())),
                "Revenue: " + scoreboard.revenue() + " (target " + config.targetRevenue() + ") "
                        + mark(config.revenueTargetMet(scoreboard.revenue())),
                "Average satisfaction: " + average(scoreboard)
                        + " (target " + config.targetAverageSatisfaction() + ") "
                        + mark(config.satisfactionTargetMet(scoreboard.paidSatisfactionSum(), scoreboard.paidCount())),
                "Served: " + scoreboard.servedCount() + " | Unhappy departures: " + scoreboard.unhappyDepartures()
                        + " | Turns used: " + config.maxTurns() + "/" + config.maxTurns());
    }

    /** Average satisfaction of paid customers to one decimal place, or N/A if nobody has paid. */
    public static String average(Scoreboard scoreboard) {
        OptionalDouble average = scoreboard.averageSatisfaction();
        return average.isPresent() ? String.format(Locale.ROOT, "%.1f", average.getAsDouble()) : "N/A";
    }

    private static String queue(List<Customer> waiting) {
        if (waiting.isEmpty()) {
            return "(empty)";
        }
        return waiting.stream()
                .map(c -> c.id() + " (" + c.typeName() + ", " + c.satisfaction() + ")")
                .collect(Collectors.joining(", "));
    }

    private static String statusLabel(CustomerStatus status) {
        return status == CustomerStatus.SERVED ? "SERVED (eating)" : status.name();
    }

    private static String orderLabel(Order order) {
        if (order == null) {
            return "no order yet";
        }
        return order.item().name() + " " + order.price() + ": " + orderStatusLabel(order);
    }

    private static String orderStatusLabel(Order order) {
        return order.status() == OrderStatus.PLACED ? "cooking " + order.progress() : order.status().name();
    }

    private static String mark(boolean met) {
        return met ? "[met]" : "[missed]";
    }
}
