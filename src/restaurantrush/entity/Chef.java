package restaurantrush.entity;

/** Advances one order by one preparation unit per turn. */
public class Chef extends Staff {

    public Chef() {
        super("Chef");
    }

    public ActionResult cook(Order order, int turn) {
        return perform(() -> {
            ActionResult check = order.canCook();
            if (!check.success()) {
                return check;
            }
            order.cook(turn);
            String ready = order.status() == OrderStatus.READY ? " - READY" : "";
            return ActionResult.ok("Chef cooks " + order.describe() + " (" + order.progress() + ")" + ready + ".");
        });
    }
}
