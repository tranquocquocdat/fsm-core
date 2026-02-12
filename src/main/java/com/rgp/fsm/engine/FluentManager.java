public class FluentManager<S, E> {
    private final List<Transition<S, E>> transitions;
    private final OutboxProducer outboxProducer; // Có thể Optional

    public FluentManager(List<Transition<S, E>> transitions, OutboxProducer outboxProducer) {
        this.transitions = transitions;
        this.outboxProducer = outboxProducer;
    }

    @Transactional(rollbackFor = Exception.class) // Đảm bảo tính nguyên tử (Atomicity)
    public S fire(String aggregateId, S currentState, E event, Map<String, Object> params) throws Exception {
        // 1. Tìm bước chuyển phù hợp
        var transition = transitions.stream()
            .filter(t -> t.from().equals(currentState) && t.event().equals(event))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Transition not found!"));

        var ctx = new TransitionContext<>(aggregateId, currentState, event, params);

        try {
            // 2. Thực thi nghiệp vụ (Command Side - Write)
            transition.action().execute(ctx);

            // 3. Ghi Outbox (Nếu có khai báo .emit)
            if (transition.eventToEmit() != null && outboxProducer != null) {
                outboxProducer.persist(aggregateId, transition.eventToEmit(), params);
            }

            // 4. Trả về trạng thái mới (Để Service lưu vào DB Entity)
            return transition.to();

        } catch (Exception ex) {
            // 5. Rollback local nếu cần (Undo)
            transition.action().undo(ctx);
            throw ex; // Re-throw để @Transactional thực hiện rollback DB
        }
    }
}