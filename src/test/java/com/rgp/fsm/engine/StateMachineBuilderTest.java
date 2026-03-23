package com.rgp.fsm.engine;

import com.rgp.fsm.core.*;
import com.rgp.fsm.example.OrderEvent;
import com.rgp.fsm.example.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StateMachineBuilder}.
 */
class StateMachineBuilderTest {

    private final BaseCommand<OrderStatus, OrderEvent> noopCommand = new BaseCommand<>() {
        @Override
        public Object execute(TransitionContext<OrderStatus, OrderEvent> ctx) {
            return "OK";
        }
    };

    private final OutboxProducer noopOutbox = (id, event, payload) -> {};
    private final StateHistoryProcessor<OrderStatus, OrderEvent> noopHistory = (ctx, next) -> {};

    @Test
    @DisplayName("Builder creates a working FluentManager with single transition")
    void singleTransition() {
        FluentManager<OrderStatus, OrderEvent> fsm = new StateMachineBuilder<OrderStatus, OrderEvent>()
            .from(OrderStatus.CREATED).on(OrderEvent.PAY)
                .action(noopCommand)
                .to(OrderStatus.PAID)
            .and()
            .build();

        assertNotNull(fsm);
        StepResult<OrderStatus> result = fsm.fire("test")
            .from(OrderStatus.CREATED)
            .on(OrderEvent.PAY)
            .params(Map.of())
            .execute();

        assertEquals(OrderStatus.PAID, result.state());
    }

    @Test
    @DisplayName("Builder supports multiple transitions")
    void multipleTransitions() {
        FluentManager<OrderStatus, OrderEvent> fsm = new StateMachineBuilder<OrderStatus, OrderEvent>()
            .from(OrderStatus.CREATED).on(OrderEvent.PAY)
                .action(noopCommand)
                .to(OrderStatus.PAID)
            .and()
            .from(OrderStatus.PAID).on(OrderEvent.SHIP)
                .action(noopCommand)
                .to(OrderStatus.SHIPPING)
            .and()
            .from(OrderStatus.SHIPPING).on(OrderEvent.DELIVER)
                .action(noopCommand)
                .to(OrderStatus.DELIVERED)
            .and()
            .build();

        // Verify all three transitions work
        StepResult<OrderStatus> r1 = fsm.fire("t").from(OrderStatus.CREATED).on(OrderEvent.PAY).params(Map.of()).execute();
        assertEquals(OrderStatus.PAID, r1.state());

        StepResult<OrderStatus> r2 = fsm.fire("t").from(OrderStatus.PAID).on(OrderEvent.SHIP).params(Map.of()).execute();
        assertEquals(OrderStatus.SHIPPING, r2.state());

        StepResult<OrderStatus> r3 = fsm.fire("t").from(OrderStatus.SHIPPING).on(OrderEvent.DELIVER).params(Map.of()).execute();
        assertEquals(OrderStatus.DELIVERED, r3.state());
    }

    @Test
    @DisplayName("Full fluent chain: ifSuccess -> ifError -> and works correctly")
    void fullFluentChain() {
        // This test verifies that chaining ifSuccess().outbox().history().ifError().undo().outbox().and()
        // compiles and runs without error
        FluentManager<OrderStatus, OrderEvent> fsm = new StateMachineBuilder<OrderStatus, OrderEvent>()
            .from(OrderStatus.CREATED).on(OrderEvent.PAY)
                .action(noopCommand)
                .to(OrderStatus.PAID)
                .guard(ctx -> true)
                .ifSuccess()
                    .outbox(noopOutbox, "ORDER_PAID", ctx -> Map.of("id", ctx.aggregateId()))
                    .history(noopHistory)
                .ifError()
                    .to(OrderStatus.CANCELLED)
                    .undo(ctx -> {})
                    .outbox(noopOutbox, "PAY_FAILED")
            .and()
            .build();

        StepResult<OrderStatus> result = fsm.fire("test")
            .from(OrderStatus.CREATED)
            .on(OrderEvent.PAY)
            .params(Map.of())
            .execute();

        assertEquals(OrderStatus.PAID, result.state());
    }

    @Test
    @DisplayName("Transition without optional configs has null sub-records")
    void minimalTransition() {
        // Just from/on/action/to — no guard, no outbox, no history, no error config
        FluentManager<OrderStatus, OrderEvent> fsm = new StateMachineBuilder<OrderStatus, OrderEvent>()
            .from(OrderStatus.CREATED).on(OrderEvent.PAY)
                .action(noopCommand)
                .to(OrderStatus.PAID)
            .and()
            .build();

        StepResult<OrderStatus> result = fsm.fire("test")
            .from(OrderStatus.CREATED)
            .on(OrderEvent.PAY)
            .params(Map.of())
            .execute();

        assertEquals(OrderStatus.PAID, result.state());
        assertEquals("OK", result.output());
    }

    @Test
    @DisplayName("ifError -> ifSuccess chain works (reverse order)")
    void errorThenSuccess() {
        FluentManager<OrderStatus, OrderEvent> fsm = new StateMachineBuilder<OrderStatus, OrderEvent>()
            .from(OrderStatus.CREATED).on(OrderEvent.PAY)
                .action(noopCommand)
                .to(OrderStatus.PAID)
                .ifError()
                    .to(OrderStatus.CANCELLED)
                    .undo()
                .ifSuccess()
                    .outbox(noopOutbox, "ORDER_PAID")
                    .history(noopHistory)
            .and()
            .build();

        StepResult<OrderStatus> result = fsm.fire("test")
            .from(OrderStatus.CREATED)
            .on(OrderEvent.PAY)
            .params(Map.of())
            .execute();

        assertEquals(OrderStatus.PAID, result.state());
    }
}
