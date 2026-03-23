package com.rgp.fsm.engine;

import com.rgp.fsm.core.*;
import com.rgp.fsm.example.OrderEvent;
import com.rgp.fsm.example.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FluentManager}.
 */
class FluentManagerTest {

    private final List<String> outboxEvents = new ArrayList<>();
    private final List<String> historyLog = new ArrayList<>();

    private final OutboxProducer testOutbox = (id, eventName, payload) ->
        outboxEvents.add(eventName + ":" + id);

    private final StateHistoryProcessor<OrderStatus, OrderEvent> testHistory = (ctx, next) ->
        historyLog.add(ctx.fromState() + "->" + next);

    private FluentManager<OrderStatus, OrderEvent> fsm;

    @BeforeEach
    void setUp() {
        outboxEvents.clear();
        historyLog.clear();

        fsm = new StateMachineBuilder<OrderStatus, OrderEvent>()
            // Transition 1: CREATED -> PAID
            .from(OrderStatus.CREATED).on(OrderEvent.PAY)
                .action(new BaseCommand<>() {
                    @Override
                    public void validate(TransitionContext<OrderStatus, OrderEvent> ctx) throws Exception {
                        Double amount = (Double) ctx.params().get("amount");
                        if (amount == null || amount <= 0) {
                            throw new Exception("Invalid amount");
                        }
                    }
                    @Override
                    public Object execute(TransitionContext<OrderStatus, OrderEvent> ctx) {
                        return Map.of("txId", "TX-001");
                    }
                })
                .to(OrderStatus.PAID)
                .ifSuccess()
                    .outbox(testOutbox, "ORDER_PAID")
                    .history(testHistory)
                .ifError()
                    .to(OrderStatus.CANCELLED)
                    .undo(ctx -> outboxEvents.add("UNDO:" + ctx.aggregateId()))
                    .outbox(testOutbox, "PAY_FAILED")
            .and()

            // Transition 2: PAID -> SHIPPING (with guard)
            .from(OrderStatus.PAID).on(OrderEvent.SHIP)
                .guard(ctx -> ctx.params().containsKey("carrier"))
                .action(new BaseCommand<>() {
                    @Override
                    public Object execute(TransitionContext<OrderStatus, OrderEvent> ctx) {
                        return "TRACK-123";
                    }
                })
                .to(OrderStatus.SHIPPING)
                .ifSuccess()
                    .outbox(testOutbox, "ORDER_SHIPPED")
            .and()
            .build();
    }

    @Test
    @DisplayName("Happy path: CREATED -> PAY -> PAID with outbox and history")
    void happyPath() {
        StepResult<OrderStatus> result = fsm.fire("ORD-1")
            .version(1)
            .from(OrderStatus.CREATED)
            .on(OrderEvent.PAY)
            .params(Map.of("amount", 100.0))
            .execute();

        assertEquals(OrderStatus.PAID, result.state());
        assertEquals(Map.of("txId", "TX-001"), result.output());
        assertTrue(outboxEvents.contains("ORDER_PAID:ORD-1"));
        assertTrue(historyLog.contains("CREATED->PAID"));
    }

    @Test
    @DisplayName("Guard rejection: PAID -> SHIP without carrier throws GuardRejectedException")
    void guardRejection() {
        assertThrows(GuardRejectedException.class, () ->
            fsm.fire("ORD-1")
                .version(2)
                .from(OrderStatus.PAID)
                .on(OrderEvent.SHIP)
                .params(Map.of()) // No carrier!
                .execute()
        );
    }

    @Test
    @DisplayName("Guard passes: PAID -> SHIP with carrier succeeds")
    void guardPasses() {
        StepResult<OrderStatus> result = fsm.fire("ORD-1")
            .version(2)
            .from(OrderStatus.PAID)
            .on(OrderEvent.SHIP)
            .params(Map.of("carrier", "GHN"))
            .execute();

        assertEquals(OrderStatus.SHIPPING, result.state());
        assertEquals("TRACK-123", result.output());
        assertTrue(outboxEvents.contains("ORDER_SHIPPED:ORD-1"));
    }

    @Test
    @DisplayName("Validation failure: triggers undo + error outbox + error state")
    void validationFailure() {
        StepResult<OrderStatus> result = fsm.fire("ORD-1")
            .version(1)
            .from(OrderStatus.CREATED)
            .on(OrderEvent.PAY)
            .params(Map.of("amount", -10.0)) // Invalid amount
            .execute();

        assertEquals(OrderStatus.CANCELLED, result.state());
        assertNull(result.output());
        assertTrue(outboxEvents.contains("UNDO:ORD-1"), "Undo should be called");
        assertTrue(outboxEvents.contains("PAY_FAILED:ORD-1"), "Error outbox should be persisted");
        assertFalse(outboxEvents.contains("ORDER_PAID:ORD-1"), "Success outbox should NOT be persisted");
    }

    @Test
    @DisplayName("Transition not found: throws TransitionNotFoundException")
    void transitionNotFound() {
        assertThrows(TransitionNotFoundException.class, () ->
            fsm.fire("ORD-1")
                .version(1)
                .from(OrderStatus.SHIPPING)
                .on(OrderEvent.PAY) // No such transition
                .params(Map.of())
                .execute()
        );
    }

    @Test
    @DisplayName("Bridge: data from step 1 flows into step 2 params")
    void bridgeDataFlow() {
        // Step 1: PAY
        StepResult<OrderStatus> payResult = fsm.fire("ORD-1")
            .version(1)
            .from(OrderStatus.CREATED)
            .on(OrderEvent.PAY)
            .params(Map.of("amount", 50.0))
            .execute();

        // Step 2: SHIP with bridge
        AtomicReference<String> capturedTxId = new AtomicReference<>();

        // Build a custom FSM that captures bridged params
        FluentManager<OrderStatus, OrderEvent> bridgeFsm = new StateMachineBuilder<OrderStatus, OrderEvent>()
            .from(OrderStatus.PAID).on(OrderEvent.SHIP)
                .action(new BaseCommand<>() {
                    @Override
                    public Object execute(TransitionContext<OrderStatus, OrderEvent> ctx) {
                        capturedTxId.set((String) ctx.params().get("txId"));
                        return "SHIPPED";
                    }
                })
                .to(OrderStatus.SHIPPING)
            .and()
            .build();

        bridgeFsm.fire("ORD-1")
            .bridge(payResult) // Bridge the Map output from PAY
            .version(2)
            .from(OrderStatus.PAID)
            .on(OrderEvent.SHIP)
            .execute();

        assertEquals("TX-001", capturedTxId.get(), "txId from step 1 should be available in step 2 params");
    }

    @Test
    @DisplayName("Command undo via callUndo flag")
    void commandUndoViaFlag() {
        AtomicBoolean undoCalled = new AtomicBoolean(false);

        FluentManager<OrderStatus, OrderEvent> fsmWithUndo = new StateMachineBuilder<OrderStatus, OrderEvent>()
            .from(OrderStatus.CREATED).on(OrderEvent.PAY)
                .action(new BaseCommand<>() {
                    @Override
                    public Object execute(TransitionContext<OrderStatus, OrderEvent> ctx) throws Exception {
                        throw new Exception("Payment gateway down!");
                    }
                    @Override
                    public void undo(TransitionContext<OrderStatus, OrderEvent> ctx) {
                        undoCalled.set(true);
                    }
                })
                .to(OrderStatus.PAID)
                .ifError()
                    .to(OrderStatus.CANCELLED)
                    .undo() // Use command's built-in undo()
            .and()
            .build();

        StepResult<OrderStatus> result = fsmWithUndo.fire("ORD-2")
            .version(1)
            .from(OrderStatus.CREATED)
            .on(OrderEvent.PAY)
            .params(Map.of("amount", 100.0))
            .execute();

        assertEquals(OrderStatus.CANCELLED, result.state());
        assertTrue(undoCalled.get(), "Command's built-in undo() should be called");
    }

    @Test
    @DisplayName("Execution failure without error config throws CommandExecutionException")
    void executionFailureNoErrorConfig() {
        FluentManager<OrderStatus, OrderEvent> fsmNoError = new StateMachineBuilder<OrderStatus, OrderEvent>()
            .from(OrderStatus.CREATED).on(OrderEvent.PAY)
                .action(new BaseCommand<>() {
                    @Override
                    public Object execute(TransitionContext<OrderStatus, OrderEvent> ctx) throws Exception {
                        throw new RuntimeException("Unexpected error");
                    }
                })
                .to(OrderStatus.PAID)
            .and()
            .build();

        assertThrows(CommandExecutionException.class, () ->
            fsmNoError.fire("ORD-3")
                .version(1)
                .from(OrderStatus.CREATED)
                .on(OrderEvent.PAY)
                .params(Map.of("amount", 100.0))
                .execute()
        );
    }

    @Test
    @DisplayName("Success outbox with custom payload builder")
    void customPayloadBuilder() {
        List<Object> capturedPayloads = new ArrayList<>();
        OutboxProducer capturingOutbox = (id, event, payload) -> capturedPayloads.add(payload);

        FluentManager<OrderStatus, OrderEvent> fsmCustomPayload = new StateMachineBuilder<OrderStatus, OrderEvent>()
            .from(OrderStatus.CREATED).on(OrderEvent.PAY)
                .action(new BaseCommand<>() {
                    @Override
                    public Object execute(TransitionContext<OrderStatus, OrderEvent> ctx) { return null; }
                })
                .to(OrderStatus.PAID)
                .ifSuccess()
                    .outbox(capturingOutbox, "ORDER_PAID",
                        ctx -> Map.of("customKey", ctx.aggregateId()))
            .and()
            .build();

        fsmCustomPayload.fire("ORD-4")
            .version(1)
            .from(OrderStatus.CREATED)
            .on(OrderEvent.PAY)
            .params(Map.of("amount", 1.0))
            .execute();

        assertEquals(1, capturedPayloads.size());
        assertEquals(Map.of("customKey", "ORD-4"), capturedPayloads.get(0));
    }
}
