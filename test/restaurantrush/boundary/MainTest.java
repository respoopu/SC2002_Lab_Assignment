package restaurantrush.boundary;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import restaurantrush.control.GameSetup;

class MainTest {

    @Test
    void inputEndingEarlyStopsTheGameWithAMessage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Main.play(new ByteArrayInputStream(new byte[0]), new PrintStream(out, true, StandardCharsets.UTF_8), false);
        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Restaurant Rush - " + GameSetup.EDITION), printed);
        assertTrue(printed.contains("Input ended during turn 1; the game was stopped."), printed);
    }
}
