package restaurantrush.entity;

import java.util.function.Supplier;

/** A staff member who can complete at most one task per turn. */
public abstract class Staff {
    private final String role;
    private boolean actedThisTurn;

    protected Staff(String role) {
        this.role = role;
    }

    public String role() {
        return role;
    }

    public boolean hasActed() {
        return actedThisTurn;
    }

    public void startTurn() {
        actedThisTurn = false;
    }

    /**
     * Runs an action if this staff member is still free this turn. The task is
     * used up only when the action succeeds, so a rejected choice can be retried.
     */
    protected ActionResult perform(Supplier<ActionResult> action) {
        if (actedThisTurn) {
            return ActionResult.fail(role + " has already completed a task this turn");
        }
        ActionResult result = action.get();
        if (result.success()) {
            actedThisTurn = true;
        }
        return result;
    }
}
