package com.rgp.fsm.example;

import com.rgp.fsm.engine.FluentManager;
import com.rgp.fsm.engine.StateMachineBuilder;
import com.rgp.fsm.engine.StepResult;
import java.util.Map;

/**
 * End-to-end example demonstrating the FSM-Core CQRS library.
 *
 * <p>Shows:</p>
 * <ul>
 *   <li>Fluent state machine configuration</li>
 *   <li>Success path with outbox + history</li>
 *   <li>Error path with undo + error outbox</li>
 *   <li>Guard predicate</li>
 *   <li>Bridge (data flow between saga steps)</li>
 * </ul>
 */
public class ExampleUsage {

    public static void main(String[] args) {
        var outbox = new ConsoleDemo.ConsoleOutboxProducer();
        var history = new ConsoleDemo.ConsoleStateHistoryProcessor();

        // 1. Configure State Machine
        FluentManager<OrderStatus, OrderEvent> fsm = new StateMachineBuilder<OrderStatus, OrderEvent>()
            .from(OrderStatus.CREATED).on(OrderEvent.PAY)
                .action(new PayOrderCommand())
                .to(OrderStatus.PAID)
                .ifSuccess()
                    .outbox(outbox, "ORDER_PAID",
                        ctx -> Map.of("id", ctx.aggregateId(), "amount", ctx.params().get("amount")))
                    .history(history)
                .ifError()
                    .to(OrderStatus.CANCELLED)
                    .undo(ctx -> System.out.println("[Undo] Rolling back payment for " + ctx.aggregateId()))
                    .outbox(outbox, "PAY_FAILED")
            .and()

            .from(OrderStatus.PAID).on(OrderEvent.SHIP)
                .guard(ctx -> ctx.params().containsKey("carrier")) // GUARD: must have carrier
                .action(new ShipOrderCommand())
                .to(OrderStatus.SHIPPING)
                .ifSuccess()
                    .outbox(outbox, "ORDER_SHIPPED")
            .and()
            .build();

        // 2. Execute with BRIDGE (auto-pass data between steps)
        System.out.println("=== FSM-CORE CQRS DEMO ===\n");

        StepResult<OrderStatus> payRes = fsm.fire("ORD-555")
            .version(1)
            .from(OrderStatus.CREATED)
            .on(OrderEvent.PAY)
            .params(Map.of("amount", 200.0))
            .execute();

        System.out.println("\n>>> Payment result: state=" + payRes.state()
            + ", output=" + payRes.output() + "\n");

        if (payRes.state() == OrderStatus.PAID) {
            // Use .bridge() to pass Map result from PAY step to SHIP step
            StepResult<OrderStatus> shipRes = fsm.fire("ORD-555")
                .bridge(payRes) // AUTO-BRIDGE: merge previous output into params
                .version(2)
                .from(OrderStatus.PAID)
                .on(OrderEvent.SHIP)
                .params(Map.of("carrier", "GiaoHangNhanh"))
                .execute();

            System.out.println("\n>>> Shipping result: state=" + shipRes.state()
                + ", tracking=" + shipRes.output());
        }
    }
}
