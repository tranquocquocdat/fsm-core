package com.rgp.fsm.core;

public interface BaseCommand<S, E> {
    Object execute(TransitionContext<S, E> ctx) throws Exception;

    default void undo(TransitionContext<S, E> ctx) {
        // Mặc định không làm gì, chỉ override khi cần rollback local
    }
}