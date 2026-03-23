package com.rgp.fsm.example;

import com.rgp.fsm.core.BaseCommand;
import com.rgp.fsm.core.TransitionContext;

/**
 * Example command: Ship an order to a carrier.
 *
 * <p>Demonstrates guard usage: the transition requires a "carrier" parameter.</p>
 */
public class ShipOrderCommand implements BaseCommand<OrderStatus, OrderEvent> {

    @Override
    public void validate(TransitionContext<OrderStatus, OrderEvent> ctx) throws Exception {
        String carrier = (String) ctx.params().get("carrier");
        if (carrier == null || carrier.isBlank()) {
            throw new Exception("Shipping failed: carrier is required");
        }
        System.out.println("[Validate] Carrier is valid: " + carrier);
    }

    @Override
    public Object execute(TransitionContext<OrderStatus, OrderEvent> ctx) throws Exception {
        System.out.println("[Action] Packaging and shipping order: " + ctx.aggregateId());
        String carrier = (String) ctx.params().get("carrier");
        System.out.println("[Action] Linked to carrier: " + carrier);
        return "TRACK-ABC-123"; // Return tracking number
    }

    @Override
    public void undo(TransitionContext<OrderStatus, OrderEvent> ctx) {
        System.out.println("[Undo] Cancelling shipment for order: " + ctx.aggregateId());
    }
}
