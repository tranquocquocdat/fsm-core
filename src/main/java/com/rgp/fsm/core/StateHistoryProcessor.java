package com.rgp.fsm.core;

/**
 * SPI for recording state transition history (audit trail / event sourcing).
 *
 * <p>Implement this interface to persist an audit log of every state change.
 * This is called on the success path, after the command executes successfully.</p>
 *
 * <p>Example implementation:</p>
 * <pre>{@code
 * @Component
 * public class JpaStateHistoryProcessor implements StateHistoryProcessor<OrderStatus, OrderEvent> {
 *     @Override
 *     public void process(TransitionContext<OrderStatus, OrderEvent> ctx, OrderStatus nextState) {
 *         historyRepository.save(new StateHistory(
 *             ctx.aggregateId(), ctx.fromState(), nextState, ctx.event(), Instant.now()
 *         ));
 *     }
 * }
 * }</pre>
 *
 * @param <S> State enum type
 * @param <E> Event enum type
 */
public interface StateHistoryProcessor<S, E> {

    /**
     * Record a state transition in the audit log.
     *
     * @param ctx       the transition context
     * @param nextState the target state after successful transition
     */
    void process(TransitionContext<S, E> ctx, S nextState);
}
