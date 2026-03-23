package com.rgp.fsm.core;

import java.util.Map;

/**
 * Immutable context passed to every command during a state transition.
 *
 * <p>Contains all the information a command needs to validate and execute,
 * including the aggregate identity, optimistic lock version, current state,
 * triggering event, and arbitrary parameters.</p>
 *
 * @param aggregateId unique identifier of the aggregate (e.g., order ID, transaction ID)
 * @param version     current version for optimistic locking (nullable)
 * @param commandId   idempotency key to prevent duplicate command processing (nullable)
 * @param fromState   the current state before this transition
 * @param event       the event that triggered this transition
 * @param params      arbitrary key-value payload data for the command
 * @param <S> State enum type
 * @param <E> Event enum type
 */
public record TransitionContext<S, E>(
    String aggregateId,
    Integer version,
    String commandId,
    S fromState,
    E event,
    Map<String, Object> params
) {
    /**
     * Convenience constructor without commandId (backward-compatible).
     */
    public TransitionContext(String aggregateId, Integer version, S fromState, E event, Map<String, Object> params) {
        this(aggregateId, version, null, fromState, event, params);
    }
}