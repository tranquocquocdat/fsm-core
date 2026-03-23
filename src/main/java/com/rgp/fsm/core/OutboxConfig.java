package com.rgp.fsm.core;

import java.util.function.Function;

/**
 * Groups outbox-related configuration for a single transition.
 *
 * <p>Used for both success and error outbox settings within a {@link Transition}.
 * Encapsulates the outbox producer, event name, and optional payload builder.</p>
 *
 * @param producer       the outbox producer to persist the event
 * @param eventName      the event type name (e.g., "ORDER_PAID")
 * @param payloadBuilder optional function to build a custom payload from the context;
 *                       if null, the raw {@code params} map is used as payload
 * @param <S> State enum type
 * @param <E> Event enum type
 */
public record OutboxConfig<S, E>(
    OutboxProducer producer,
    String eventName,
    Function<TransitionContext<S, E>, Object> payloadBuilder
) {
    /**
     * Creates an OutboxConfig without a custom payload builder.
     */
    public static <S, E> OutboxConfig<S, E> of(OutboxProducer producer, String eventName) {
        return new OutboxConfig<>(producer, eventName, null);
    }

    /**
     * Creates an OutboxConfig with a custom payload builder.
     */
    public static <S, E> OutboxConfig<S, E> of(OutboxProducer producer, String eventName,
                                                 Function<TransitionContext<S, E>, Object> payloadBuilder) {
        return new OutboxConfig<>(producer, eventName, payloadBuilder);
    }
}
