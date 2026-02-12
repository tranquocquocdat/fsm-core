package com.rgp.fsm.core;

import java.util.function.Predicate;

public record Transition<S, E>(
    S from,
    E event,
    S to,
    BaseCommand<S, E> action, // Đã có Generic
    com.rgp.fsm.core.OutboxProducer outboxProducer,
    String eventToEmit, 
    java.util.function.Function<TransitionContext<S, E>, Object> payloadBuilder,
    S errorState,
    com.rgp.fsm.core.StateHistoryProcessor<S, E> historyProcessor,
    com.rgp.fsm.core.OutboxProducer errorOutboxProducer,
    String errorEventToEmit,
    java.util.function.Function<TransitionContext<S, E>, Object> errorPayloadBuilder,
    Predicate<TransitionContext<S, E>> guard,
    BaseCommand<S, E> undoAction, // Đã có Generic
    boolean callUndo
) {}