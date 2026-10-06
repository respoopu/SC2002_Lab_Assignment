package restaurantrush.entity;

/**
 * A once-per-game promotion the manager may switch on at the start of a turn.
 * It lasts that turn and the next. Orders taken while it is active get 20%
 * off (after any customer discount), and customers still waiting for a table
 * or food recover 15 satisfaction at the start of each active turn.
 */
public class HappyHour {
    public static final int DISCOUNT_PERCENT = 20;
    public static final int RECOVERY = 15;
    public static final int DURATION_TURNS = 2;

    private boolean used;
    private int turnsRemaining;

    public boolean isAvailable() {
        return !used;
    }

    public boolean isActive() {
        return turnsRemaining > 0;
    }

    /** Active turns left, counting the current one. */
    public int turnsRemaining() {
        return turnsRemaining;
    }

    public ActionResult activate() {
        if (used) {
            return ActionResult.fail("Happy Hour has already been used this game");
        }
        used = true;
        turnsRemaining = DURATION_TURNS;
        return ActionResult.ok("Happy Hour is ON for this turn and the next: new orders get "
                + DISCOUNT_PERCENT + "% off and waiting customers recover " + RECOVERY + " satisfaction each turn.");
    }

    /** Called once at the end of every turn. */
    public void endTurn() {
        if (turnsRemaining > 0) {
            turnsRemaining--;
        }
    }

    /** The price after the Happy Hour discount, or unchanged when it is not active. */
    public Money adjust(Money price) {
        return isActive() ? price.percentOff(DISCOUNT_PERCENT) : price;
    }

    public String status() {
        if (isActive()) {
            return "ACTIVE - " + turnsRemaining + (turnsRemaining == 1 ? " turn" : " turns")
                    + " left including this one";
        }
        return used ? "used" : "available (once per game)";
    }
}
