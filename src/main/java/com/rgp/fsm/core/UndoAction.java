package com.rgp.fsm.core;

/**
 * Functional interface for undo actions.
 * Unlike BaseCommand (which has execute() returning Object),
 * this interface's accept() method returns void, making it
 * compatible with void-returning lambdas like:
 *   ctx -> System.out.println("Rolling back...")
 */
@FunctionalInterface
public interface UndoAction<S, E> {
    void accept(TransitionContext<S, E> ctx);
}
