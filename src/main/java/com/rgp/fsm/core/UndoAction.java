package com.rgp.fsm.core;

/**
 * Functional interface for custom undo/compensation actions.
 *
 * <p>Use this when you want to define undo logic inline via a lambda
 * instead of overriding {@link BaseCommand#undo(TransitionContext)}:</p>
 *
 * <pre>{@code
 * .ifError()
 *     .undo(ctx -> walletService.refund(ctx.aggregateId()))
 * }</pre>
 *
 * <p>Unlike {@link BaseCommand#execute(TransitionContext)} which returns {@code Object},
 * this interface's {@link #accept(TransitionContext)} returns {@code void},
 * making it compatible with void-returning lambdas.</p>
 *
 * @param <S> State enum type
 * @param <E> Event enum type
 */
@FunctionalInterface
public interface UndoAction<S, E> {

    /**
     * Execute the undo/compensation logic.
     *
     * @param ctx the transition context at the time of failure
     */
    void accept(TransitionContext<S, E> ctx);
}
