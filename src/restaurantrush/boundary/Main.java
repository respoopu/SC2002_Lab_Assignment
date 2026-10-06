package restaurantrush.boundary;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import restaurantrush.control.GameController;
import restaurantrush.control.GameSetup;

/** Starts a base-setting game on the console. Pass --echo when replaying a scripted run. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        boolean echo = Arrays.asList(args).contains("--echo");
        play(System.in, System.out, echo);
    }

    static void play(InputStream in, PrintStream out, boolean echo) {
        GameController game = GameSetup.baseGame(new GameCLI(in, out, echo));
        out.println("Restaurant Rush - " + GameSetup.EDITION);
        out.println("You are the manager. Type the number of your choice at each prompt.");
        try {
            game.run();
        } catch (InputEndedException e) {
            out.println();
            out.println("Input ended during turn " + game.currentTurn() + "; the game was stopped.");
        }
    }
}
