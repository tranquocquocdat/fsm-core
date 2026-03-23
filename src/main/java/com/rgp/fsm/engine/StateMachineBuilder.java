package com.rgp.fsm.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import com.rgp.fsm.core.Transition;
import com.rgp.fsm.core.TransitionContext;
import com.rgp.fsm.core.BaseCommand;
import com.rgp.fsm.core.UndoAction;
import com.rgp.fsm.core.OutboxProducer;
import com.rgp.fsm.core.OutboxConfig;
import com.rgp.fsm.core.ErrorConfig;
import com.rgp.fsm.core.StateHistoryProcessor;

/**
 * Fluent builder for constructing a state machine configuration.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * FluentManager<OrderStatus, OrderEvent> fsm = new StateMachineBuilder<OrderStatus, OrderEvent>()
 *     .from(CREATED).on(PAY)
 *         .action(new PayOrderCommand())
 *         .to(PAID)
 *         .ifSuccess()
 *             .outbox(outbox, "ORDER_PAID")
 *             .history(history)
 *         .ifError()
 *             .to(CANCELLED)
 *             .undo(ctx -> compensate(ctx))
 *             .outbox(outbox, "PAY_FAILED")
 *     .and()
 *     .build();
 * }</pre>
 *
 * @param <S> State enum type
 * @param <E> Event enum type
 */
public class StateMachineBuilder<S, E> {
    private final List<Transition<S, E>> transitions = new ArrayList<>();

    /**
     * Start defining a transition from the given source state.
     *
     * @param state the source state
     * @return a transition builder
     */
    public TransitionBuilder from(S state) {
        return new TransitionBuilder(state);
    }

    /**
     * Builder for configuring a single transition.
     */
    public class TransitionBuilder {
        private final S from;
        private E event;
        private S to;
        private BaseCommand<S, E> action;
        private Predicate<TransitionContext<S, E>> guard;

        // Success config fields
        private OutboxProducer outboxProducer;
        private String eventToEmit;
        private Function<TransitionContext<S, E>, Object> payloadBuilder;
        private StateHistoryProcessor<S, E> historyProcessor;

        // Error config fields
        private S errorState;
        private UndoAction<S, E> undoAction;
        private boolean callUndo;
        private OutboxProducer errorOutboxProducer;
        private String errorEventToEmit;
        private Function<TransitionContext<S, E>, Object> errorPayloadBuilder;

        public TransitionBuilder(S from) { this.from = from; }

        /** Set the triggering event. */
        public TransitionBuilder on(E event) { this.event = event; return this; }

        /** Set the target state on success. */
        public TransitionBuilder to(S to) { this.to = to; return this; }

        /** Set the command to execute during this transition. */
        public TransitionBuilder action(BaseCommand<S, E> action) { this.action = action; return this; }

        /** Set a guard predicate — transition is rejected if guard returns false. */
        public TransitionBuilder guard(Predicate<TransitionContext<S, E>> guard) { this.guard = guard; return this; }

        /** Start configuring the success path. */
        public SuccessConfigurator ifSuccess() {
            return new SuccessConfigurator();
        }

        /** Start configuring the error path. */
        public ErrorConfigurator ifError() {
            return new ErrorConfigurator();
        }

        /**
         * Configurator for success-path infrastructure (outbox, history).
         */
        public class SuccessConfigurator {

            /** Configure outbox event on success. */
            public SuccessConfigurator outbox(OutboxProducer infra, String eventName) {
                outboxProducer = infra;
                eventToEmit = eventName;
                return this;
            }

            /** Configure outbox event on success with a custom payload builder. */
            public SuccessConfigurator outbox(OutboxProducer infra, String eventName, Function<TransitionContext<S, E>, Object> builder) {
                outboxProducer = infra;
                eventToEmit = eventName;
                payloadBuilder = builder;
                return this;
            }

            /** Configure history/audit recording on success. */
            public SuccessConfigurator history(StateHistoryProcessor<S, E> infra) {
                historyProcessor = infra;
                return this;
            }

            /** Finalize this transition and return to the top-level builder. */
            public StateMachineBuilder<S, E> and() { return TransitionBuilder.this.and(); }

            /** Switch to error path configuration. */
            public ErrorConfigurator ifError() { return new ErrorConfigurator(); }

            /** Shortcut to finalize this transition. */
            public StateMachineBuilder<S, E> buildTransition() { return and(); }
        }

        /**
         * Configurator for error-path infrastructure (undo, error state, error outbox).
         */
        public class ErrorConfigurator {

            /** Set the state to transition to on error. */
            public ErrorConfigurator to(S state) {
                errorState = state;
                return this;
            }

            /** Enable the command's built-in {@link BaseCommand#undo(TransitionContext)} method. */
            public ErrorConfigurator undo() {
                TransitionBuilder.this.callUndo = true;
                return this;
            }

            /** Define custom undo/compensation logic via a lambda. */
            public ErrorConfigurator undo(UndoAction<S, E> action) {
                TransitionBuilder.this.undoAction = action;
                return this;
            }

            /** Configure outbox event on error. */
            public ErrorConfigurator outbox(OutboxProducer infra, String eventName) {
                errorOutboxProducer = infra;
                errorEventToEmit = eventName;
                return this;
            }

            /** Configure outbox event on error with a custom payload builder. */
            public ErrorConfigurator outbox(OutboxProducer infra, String eventName, Function<TransitionContext<S, E>, Object> builder) {
                errorOutboxProducer = infra;
                errorEventToEmit = eventName;
                errorPayloadBuilder = builder;
                return this;
            }

            /** Finalize this transition and return to the top-level builder. */
            public StateMachineBuilder<S, E> and() { return TransitionBuilder.this.and(); }

            /** Switch to success path configuration. */
            public SuccessConfigurator ifSuccess() { return new SuccessConfigurator(); }

            /** Shortcut to finalize this transition. */
            public StateMachineBuilder<S, E> buildTransition() { return and(); }
        }

        /**
         * Finalize this transition: assemble sub-records and add to the transition list.
         *
         * @return the parent StateMachineBuilder for chaining
         */
        public StateMachineBuilder<S, E> and() {
            OutboxConfig<S, E> successOutbox = (outboxProducer != null && eventToEmit != null)
                ? new OutboxConfig<>(outboxProducer, eventToEmit, payloadBuilder)
                : null;

            OutboxConfig<S, E> errorOutbox = (errorOutboxProducer != null && errorEventToEmit != null)
                ? new OutboxConfig<>(errorOutboxProducer, errorEventToEmit, errorPayloadBuilder)
                : null;

            ErrorConfig<S, E> errorConfig = (errorState != null || undoAction != null || callUndo || errorOutbox != null)
                ? new ErrorConfig<>(errorState, undoAction, callUndo, errorOutbox)
                : null;

            transitions.add(new Transition<>(
                from, event, to, action, guard,
                successOutbox, historyProcessor, errorConfig
            ));
            return StateMachineBuilder.this;
        }
    }

    /**
     * Build the {@link FluentManager} with all configured transitions.
     *
     * @return a fully configured FluentManager ready to fire transitions
     */
    public FluentManager<S, E> build() {
        return new FluentManager<>(transitions);
    }
}
