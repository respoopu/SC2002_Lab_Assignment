package restaurantrush.entity;

import java.util.ArrayList;
import java.util.List;

/** Events that happened during the current turn, waiting to be displayed. */
public class TurnLog {
    private final List<String> lines = new ArrayList<>();

    public void add(String line) {
        lines.add(line);
    }

    /** Returns every pending line and clears the log. */
    public List<String> drain() {
        List<String> drained = List.copyOf(lines);
        lines.clear();
        return drained;
    }
}
