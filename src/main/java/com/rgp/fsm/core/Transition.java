package com.rgp.fsm.core;

import java.util.function.Predicate;

/**
 * Immutable definition of a single state transition in the finite state machine.
 *
 * <p>A transition defines: "When in state {@code from} and event {@code event} occurs,
 * execute {@code action} and move to state {@code to}."</p>
 *
 * <p>Additional infrastructure concerns (outbox, history, error handling) are grouped
 * into sub-records for clarity.</p>
 *
 * @param from             source state
 * @param event            triggering event
 * @param to               target state (on success)
 * @param action           the command to execute during this transition
 * @param guard            optional precondition predicate — transition is rejected if guard returns false
 * @param successOutbox    optional outbox configuration for the success path
 * @param historyProcessor optional audit/history recorder
 * @param errorConfig      optional error handling configuration (undo, error state, error outbox)
 * @param <S> State enum type
 * @param <E> Event enum type
 */
public record Transition<S, E>(
    S from,
    E event,
    S to,
    BaseCommand<S, E> action,
    Predicate<TransitionContext<S, E>> guard,
    OutboxConfig<S, E> successOutbox,
    StateHistoryProcessor<S, E> historyProcessor,
    ErrorConfig<S, E> errorConfig
) {}