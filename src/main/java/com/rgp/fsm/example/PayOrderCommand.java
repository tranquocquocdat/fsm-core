package com.rgp.fsm.example;

import com.rgp.fsm.core.BaseCommand;
import com.rgp.fsm.core.TransitionContext;
import java.util.Map;

/**
 * Example command: Process a payment for an order.
 *
 * <p>Demonstrates the full command lifecycle: validate → execute → undo.</p>
 */
public class PayOrderCommand implements BaseCommand<OrderStatus, OrderEvent> {

    @Override
    public void validate(TransitionContext<OrderStatus, OrderEvent> ctx) throws Exception {
        Double amount = (Double) ctx.params().get("amount");
        if (amount == null || amount <= 0) {
            throw new Exception("Payment failed: amount must be greater than 0");
        }
        System.out.println("[Validate] Payment data is valid.");
    }

    @Override
    public Object execute(TransitionContext<OrderStatus, OrderEvent> ctx) throws Exception {
        System.out.println("[Action] Processing payment for order: " + ctx.aggregateId());
        Double amount = (Double) ctx.params().get("amount");
        System.out.println("[Action] Charged via payment gateway: " + amount + " VND");

        // Return a Map of data that can be "bridged" to the next step
        return Map.of(
            "txId", "TX-" + System.currentTimeMillis(),
            "paidAt", java.time.LocalDateTime.now(),
            "status", "SUCCESS"
        );
    }

    @Override
    public void undo(TransitionContext<OrderStatus, OrderEvent> ctx) {
        System.out.println("[Undo] Refunding payment for order: " + ctx.aggregateId());
    }
}
