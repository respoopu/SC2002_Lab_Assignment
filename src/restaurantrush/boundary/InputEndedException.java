package restaurantrush.boundary;

/** Thrown when standard input ends before the game does, e.g. a scripted run that is too short. */
public class InputEndedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InputEndedException() {
        super("Input ended before the game finished");
    }
}
