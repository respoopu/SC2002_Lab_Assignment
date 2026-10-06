package restaurantrush.boundary;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Replays runs/*.txt through the real game and checks the documented results. */
class RunScriptsTest {

    private static String play(String script) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = Files.newInputStream(Path.of("runs", script))) {
            Main.play(in, new PrintStream(out, true, StandardCharsets.UTF_8), true);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static void assertContains(String transcript, String expected) {
        assertTrue(transcript.contains(expected),
                () -> "Expected \"" + expected + "\" in transcript ending:\n"
                        + transcript.substring(Math.max(0, transcript.length() - 3000)));
    }

    @Test
    void victoryRunReproduces() throws IOException {
        String out = play("victory.txt");
        assertContains(out, "GAME OVER: VICTORY");
        assertContains(out, "Paid customers: 5 (target 4) [met]");
        assertContains(out, "Revenue: $86.40 (target $45.00) [met]");
        assertContains(out, "Average satisfaction: 89.4 (target 60) [met]");
        assertContains(out, "C3 (Critic): served fresh - no complaints.");
        assertContains(out, "C5 orders Salad + Pasta ($24.30, 3 units) - bill $24.30.");
        assertContains(out, "Happy Hour: ACTIVE - 1 turn left including this one");
        assertContains(out, "Served: 5 | Unhappy departures: 0 | Turns used: 18/18");
        assertFalse(out.contains("Not allowed"), "the victory script should only make legal moves");
        assertFalse(out.contains("Input ended"), "the victory script is too short");
    }

    @Test
    void defeatRunReproduces() throws IOException {
        String out = play("defeat.txt");
        assertContains(out, "Invalid input 'abc'");
        assertContains(out, "There is nothing to choose from.");
        assertContains(out, "GAME OVER: DEFEAT");
        assertContains(out, "Paid customers: 0 (target 4) [missed]");
        assertContains(out, "Revenue: $0.00 (target $45.00) [missed]");
        assertContains(out, "Average satisfaction: N/A (target 60) [missed]");
        assertContains(out, "Served: 0 | Unhappy departures: 2 | Turns used: 18/18");
        assertFalse(out.contains("Input ended"), "the defeat script is too short");
    }

    @Test
    void noHappyHourRunReproduces() throws IOException {
        String out = play("what-if-no-happy-hour.txt");
        assertContains(out, "C3 (Critic): served fresh - no complaints.");
        assertContains(out, "Cashier collects $18.00 from C3 (satisfaction 76 recorded)");
        assertContains(out, "GAME OVER: VICTORY");
        assertContains(out, "Revenue: $86.40 (target $45.00) [met]");
        assertContains(out, "Average satisfaction: 84.6 (target 60) [met]");
        assertFalse(out.contains("Happy Hour is ON"), "this script never switches Happy Hour on");
        assertFalse(out.contains("Not allowed"), "this script should only make legal moves");
        assertFalse(out.contains("Input ended"), "this script is too short");
    }

    @Test
    void criticServedLateRunReproduces() throws IOException {
        String out = play("what-if-critic-served-late.txt");
        assertContains(out, "C3 (Critic): cold food! -20 satisfaction (now 68).");
        assertContains(out, "Cashier collects $18.00 from C3 (satisfaction 68 recorded)");
        assertContains(out, "Cashier collects $21.60 from C4 (satisfaction 68 recorded)");
        assertContains(out, "GAME OVER: VICTORY");
        assertContains(out, "Paid customers: 5 (target 4) [met]");
        assertContains(out, "Average satisfaction: 79.8 (target 60) [met]");
        assertFalse(out.contains("Not allowed"), "this script should only make legal moves");
        assertFalse(out.contains("Input ended"), "this script is too short");
    }

    @Test
    void hostIdleRunReproduces() throws IOException {
        String out = play("what-if-host-idle.txt");
        assertContains(out, "Host waits.");
        assertContains(out, "C5 finished eating and is READY_TO_PAY.");
        assertContains(out, "GAME OVER: VICTORY");
        assertContains(out, "Paid customers: 4 (target 4) [met]");
        assertContains(out, "Revenue: $62.10 (target $45.00) [met]");
        assertContains(out, "Average satisfaction: 92.8 (target 60) [met]");
        assertFalse(out.contains("from C5"), "C5 should not be able to pay within 18 turns");
        assertFalse(out.contains("Not allowed"), "this script should only make legal moves");
        assertFalse(out.contains("Input ended"), "this script is too short");
    }
}
