package com.rgp.fsm.core;

/**
 * Base interface for all CQRS commands in the FSM engine.
 *
 * <p>Each command follows a three-phase lifecycle:</p>
 * <ol>
 *   <li>{@link #validate(TransitionContext)} — Business rule validation (throws on failure)</li>
 *   <li>{@link #execute(TransitionContext)} — Perform the actual state change / side effects</li>
 *   <li>{@link #undo(TransitionContext)} — Compensation logic for rollback scenarios</li>
 * </ol>
 *
 * <p>Implement this interface for each domain command (e.g., PayOrderCommand, ShipOrderCommand).</p>
 *
 * @param <S> State enum type (e.g., OrderStatus)
 * @param <E> Event enum type (e.g., OrderEvent)
 */
public interface BaseCommand<S, E> {

    /**
     * Phase 1: Validate business rules before execution.
     *
     * <p>Throw an exception if the data is invalid. The engine will catch it
     * and trigger the error handling path (undo + error outbox).</p>
     *
     * @param ctx the transition context containing aggregate ID, state, event, and params
     * @throws Exception if validation fails
     */
    default void validate(TransitionContext<S, E> ctx) throws Exception {
        // Default: pass-through (no validation)
    }

    /**
     * Phase 2: Execute the command's core business logic.
     *
     * <p>This is where you perform the actual work: update database, call external services, etc.
     * The returned object is captured in {@link com.rgp.fsm.engine.StepResult#output()}
     * and can be passed to the next step via {@code bridge()}.</p>
     *
     * @param ctx the transition context
     * @return the command output (can be any object, or null)
     * @throws Exception if execution fails — triggers error handling path
     */
    Object execute(TransitionContext<S, E> ctx) throws Exception;

    /**
     * Phase 3: Compensation / undo logic.
     *
     * <p>Called automatically when execution fails and either:
     * <ul>
     *   <li>{@code .ifError().undo()} is configured (calls this method), or</li>
     *   <li>{@code .ifError().undo(ctx -> ...)} is configured (calls the lambda instead)</li>
     * </ul>
     *
     * @param ctx the transition context at the time of failure
     */
    default void undo(TransitionContext<S, E> ctx) {
        // Default: no-op
    }
}