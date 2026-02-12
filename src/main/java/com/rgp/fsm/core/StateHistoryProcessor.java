package com.rgp.fsm.core;

public interface StateHistoryProcessor<S, E> {
    void process(TransitionContext<S, E> ctx, S nextState);
}
