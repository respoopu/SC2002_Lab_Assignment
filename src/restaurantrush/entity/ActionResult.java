package restaurantrush.entity;

/** Outcome of a staff action: success with a description, or failure with the reason. */
public record ActionResult(boolean success, String message) {

    public static ActionResult ok(String message) {
        return new ActionResult(true, message);
    }

    public static ActionResult fail(String reason) {
        return new ActionResult(false, reason);
    }
}
