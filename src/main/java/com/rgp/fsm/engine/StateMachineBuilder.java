import java.util.ArrayList;
import java.util.List;
import com.rgp.fsm.core.Transition;
import com.rgp.fsm.core.TransitionContext;
import com.rgp.fsm.core.BaseCommand;
import com.rgp.fsm.core.OutboxProducer;
import com.rgp.fsm.core.StateHistoryProcessor;

public class StateMachineBuilder<S, E> {
    private final List<Transition<S, E>> transitions = new ArrayList<>();

    public TransitionBuilder from(S state) {
        return new TransitionBuilder(state);
    }

    public class TransitionBuilder {
        private final S from;
        private E event;
        private S to;
        private BaseCommand<S, E> action;
        private java.util.function.Predicate<TransitionContext<S, E>> guard;
        
        // Success Config
        private OutboxProducer outboxProducer;
        private String eventToEmit;
        private java.util.function.Function<TransitionContext<S, E>, Object> payloadBuilder;
        private StateHistoryProcessor<S, E> historyProcessor;
        
        // Error Config
        private S errorState;
        private BaseCommand<S, E> undoAction;
        private boolean callUndo; // Cờ hiệu kích hoạt undo
        private OutboxProducer errorOutboxProducer;
        private String errorEventToEmit;
        private java.util.function.Function<TransitionContext<S, E>, Object> errorPayloadBuilder;

        public TransitionBuilder(S from) { this.from = from; }
        public TransitionBuilder on(E event) { this.event = event; return this; }
        public TransitionBuilder to(S to) { this.to = to; return this; }
        public TransitionBuilder action(BaseCommand<S, E> action) { this.action = action; return this; }
        public TransitionBuilder guard(java.util.function.Predicate<TransitionContext<S, E>> guard) { this.guard = guard; return this; }
        
        public SuccessConfigurator ifSuccess() {
            return new SuccessConfigurator();
        }

        public ErrorConfigurator ifError() {
            return new ErrorConfigurator();
        }

        public class SuccessConfigurator {
            public SuccessConfigurator outbox(OutboxProducer infra, String eventName) {
                outboxProducer = infra;
                eventToEmit = eventName;
                return this;
            }

            public SuccessConfigurator outbox(OutboxProducer infra, String eventName, java.util.function.Function<TransitionContext<S, E>, Object> builder) {
                outboxProducer = infra;
                eventToEmit = eventName;
                payloadBuilder = builder;
                return this;
            }

            public SuccessConfigurator history(StateHistoryProcessor<S, E> infra) {
                historyProcessor = infra;
                return this;
            }

            public TransitionBuilder and() { return TransitionBuilder.this; }
            public ErrorConfigurator ifError() { return new ErrorConfigurator(); }
            public StateMachineBuilder<S, E> buildTransition() { return and().and(); }
        }

        public class ErrorConfigurator {
            public ErrorConfigurator to(S state) {
                errorState = state;
                return this;
            }

            // Gọi hàm undo() mặc định của Command
            public ErrorConfigurator undo() {
                this.callUndo = true;
                return this;
            }

            // Định nghĩa logic undo mới bằng Lambda/Command
            public ErrorConfigurator undo(BaseCommand<S, E> action) {
                this.undoAction = action;
                return this;
            }

            public ErrorConfigurator outbox(OutboxProducer infra, String eventName) {
                errorOutboxProducer = infra;
                errorEventToEmit = eventName;
                return this;
            }

            public ErrorConfigurator outbox(OutboxProducer infra, String eventName, java.util.function.Function<TransitionContext<S, E>, Object> builder) {
                errorOutboxProducer = infra;
                errorEventToEmit = eventName;
                errorPayloadBuilder = builder;
                return this;
            }

            public TransitionBuilder and() { return TransitionBuilder.this; }
            public SuccessConfigurator ifSuccess() { return new SuccessConfigurator(); }
            public StateMachineBuilder<S, E> buildTransition() { return and().and(); }
        }

        public StateMachineBuilder<S, E> and() {
            transitions.add(new Transition<>(from, event, to, action, outboxProducer, eventToEmit, payloadBuilder, errorState, historyProcessor, errorOutboxProducer, errorEventToEmit, errorPayloadBuilder, guard, undoAction, callUndo));
            return StateMachineBuilder.this;
        }
    }

    public FluentManager<S, E> build() {
        return new FluentManager<>(transitions);
    }
}
