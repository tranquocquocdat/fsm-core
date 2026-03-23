package com.rgp.fsm.core;

/**
 * Groups error-handling configuration for a single transition.
 *
 * <p>Encapsulates all error-path behavior:</p>
 * <ul>
 *   <li>{@code errorState} — the state to transition to on failure</li>
 *   <li>{@code undoAction} — custom compensation lambda</li>
 *   <li>{@code callUndo} — flag to call the command's built-in {@link BaseCommand#undo(TransitionContext)}</li>
 *   <li>{@code outbox} — outbox config for error events (e.g., "PAY_FAILED")</li>
 * </ul>
 *
 * @param errorState the state to move to when an error occurs (nullable — if null, exception is re-thrown)
 * @param undoAction custom undo lambda (nullable — takes priority over callUndo)
 * @param callUndo   if true and undoAction is null, calls {@link BaseCommand#undo(TransitionContext)}
 * @param outbox     outbox config for error events (nullable)
 * @param <S> State enum type
 * @param <E> Event enum type
 */
public record ErrorConfig<S, E>(
    S errorState,
    UndoAction<S, E> undoAction,
    boolean callUndo,
    OutboxConfig<S, E> outbox
) {
    /**
     * Returns {@code true} if any error handling is configured.
     */
    public boolean hasErrorHandling() {
        return errorState != null || undoAction != null || callUndo || outbox != null;
    }
}
