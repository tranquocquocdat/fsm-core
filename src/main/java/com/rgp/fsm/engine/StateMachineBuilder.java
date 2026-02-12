public class StateMachineBuilder<S, E> {
    private final List<Transition<S, E>> transitions = new ArrayList<>();

    public TransitionBuilder from(S state) {
        return new TransitionBuilder(state);
    }

    public class TransitionBuilder {
        private final S from;
        private E event;
        private S to;
        private BaseCommand action;
        private String eventToEmit;

        public TransitionBuilder(S from) { this.from = from; }
        public TransitionBuilder on(E event) { this.event = event; return this; }
        public TransitionBuilder to(S to) { this.to = to; return this; }
        public TransitionBuilder action(BaseCommand action) { this.action = action; return this; }
        public TransitionBuilder emit(String eventName) { this.eventToEmit = eventName; return this; }

        public StateMachineBuilder<S, E> and() {
            transitions.add(new Transition<>(from, event, to, action, eventToEmit));
            return StateMachineBuilder.this;
        }
    }

    public FluentManager<S, E> build(OutboxProducer outboxProducer) {
        return new FluentManager<>(transitions, outboxProducer);
    }
}