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
        assertContains(out, "Revenue: $64.50 (target $45.00) [met]");
        assertContains(out, "Average satisfaction: 78.0 (target 60) [met]");
        assertContains(out, "C3 (Critic): served fresh - no complaints.");
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
}
